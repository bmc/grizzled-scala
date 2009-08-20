/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2009, Brian M. Clapper
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

  * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

  * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  * Neither the names "clapper.org", "Grizzled Scala Library", nor the
    names of its contributors may be used to endorse or promote products
    derived from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  ---------------------------------------------------------------------------
*/

/**
 * JavaReadline implementation of the traits defined in the base readline
 * package. Handles both GNU Readline and Editline.
 */
package grizzled.readline.javareadline

import grizzled.readline._
import grizzled.collection._
import grizzled.collection.implicits._
import grizzled.string.implicits._

import org.gnu.readline.{Readline => JavaReadline,
                         ReadlineCompleter => JavaReadlineCompleter,
                         ReadlineLibrary => JavaReadlineLibrary}

/**
 * History implementation that wraps the JavaReadline history API.
 */
private[javareadline] class ReadlineHistory extends History
{
    private var maxSize = Integer.MAX_VALUE

    /**
     * Get the contents of the history buffer, in a list.
     *
     * @return the history lines
     */
    def get: List[String] =
    {
        import _root_.java.util.ArrayList

        val history = new ArrayList[String]

        JavaReadline.getHistory(history)
        history.toList
    }

    /**
     * Clear the history buffer
     */
    def clear = JavaReadline.clearHistory

    /**
     * Get the last (i.e., most recent) entry from the buffer.
     *
     * @return the most recent entry, as an <tt>Option</tt>, or
     *         <tt>None</tt> if the history buffer is empty
     */
    def last: Option[String] =
    {
        val history = get
        history match
        {
            case Nil => None
            case _   => Some(history.last)
        }
    }

    /**
     * Get the current number of entries in the history buffer.
     *
     * @return the size of the history buffer
     */
    def size: Int = get.length

    /**
     * Get maximum history size.
     *
     * @return the current max history size, or 0 for unlimited.
     */
    def max: Int = maxSize

    /**
     * Set maximum history size.
     *
     * @param newSize the new max history size, or 0 for unlimited.
     */
    def max_=(newSize: Int)
    {
        maxSize = newSize
        ensureMaxSize
    }

    /**
     * Unconditionally appends the specified line to the history.
     *
     * @param line  the line to add
     */
    protected def append(line: String) =
    {
        JavaReadline.addToHistory(line)
        ensureMaxSize
    }

    private def ensureMaxSize: Unit =
    {
        val history = get
        if (history.length > maxSize)
        {
            val newHistory = history drop (history.length - maxSize)
            clear
            for (line <- newHistory.reverse)
                append(line)
        }
    }
}

/**
 * JavaReadline implementation of the Readline trait.
 */
private[readline] abstract class JavaReadlineImpl(appName: String,
                                                  readlineName: String,
                                                  val autoAddHistory: Boolean,
                                                  library: JavaReadlineLibrary)
    extends Readline
{
    val name = readlineName
    val history = new ReadlineHistory
    val self = this

    JavaReadline.load(library)
    JavaReadline.initReadline(appName)
    JavaReadline.setCompleter(rlCompleter)

    Runtime.getRuntime.addShutdownHook(
        new Thread
        {
            override def run = JavaReadline.cleanup
        }
    )

    subclassInit()

    /**
     * Subclass-specific initialization.
     */
    protected def subclassInit(): Unit = return

    object rlCompleter extends JavaReadlineCompleter with CompleterHelper
    {
        private var iterator: Iterator[String] = null

        def completer(token: String, state: Int): String =
        {
            // Note that libreadline-java doesn't supply a cursor, even
            // though it's possible to get one from GNU Readline and
            // Editline. Simulate one by assuming that the token being
            // completed is the last match for the token in the line. This
            // will fail under certain circumstances, but that's the best
            // we can do.

            def getTokens(line: String): List[CompletionToken] =
            {
                // Split the token list around the first occurrence of a
                // match for the token (which, because the list is reversed,
                // i really the last occurrence of such a  match).
                val revTokens = line.tokenize.reverse
                val (a, b) = revTokens.break(_.startsWith(token))
                val lastIsWhite = Character.isWhitespace(line.last)

                (a, b) match
                {
                    case (Nil, Nil) =>
                        // No tokens (empty list). Cursor is at the beginning.
                        List(Cursor)

                    case (Nil, list) =>
                        // Matches first token (which is really the last token).
                        val matchToken = list(0)
                        val rest = list drop 1
                        // Avoid stray leading Delim
                        rest match
                        {
                            case Nil if lastIsWhite =>
                                List(LineToken(matchToken), Delim, Cursor)
                            case Nil =>
                                List(LineToken(matchToken), Cursor)
                            case _ if lastIsWhite =>
                                mapWithDelims(rest.reverse) ++ 
                                List(Delim, LineToken(matchToken), Delim, 
                                     Cursor)
                            case _  =>
                                mapWithDelims(rest.reverse) ++ 
                                List(Delim, LineToken(matchToken), Cursor)
                        }

                    case (list, Nil) =>
                        // No match anywhere. Cursor goes at end. Handle
                        // white space at end of line.
                        if (lastIsWhite)
                            mapWithDelims(list.reverse) ++ List(Delim, Cursor)
                        else
                            mapWithDelims(list.reverse) ++ List(Cursor)

                    case (list1, list2) =>
                        // Cursor is between the elements.
                        val l2 = mapWithDelims(list2.reverse)
                        val l1 = mapWithDelims(list1.reverse)
                        l2 ++ List(Cursor, Delim) ++ l1
                }
            }

            // libreadline-java uses the goofy GNU Readline semantics,
            // where multiple calls are made, with increasing values of the
            // "state" variable, to retrieve each completion value.
            // Returning null tells GNU Readline to stop asking for
            // results. This approach is suitable when there are huge
            // numbers of completions and minimal memory, but it's overly
            // complicated. To map it into an approach that's more
            // consistent with other readline libraries, we load all the
            // completions at state 0 (the first call), load an iterator,
            // and bleed that iterator on subsequent calls.

            if (state == 0)
            {
                // First call to completer. Get list of matches.

                val line = JavaReadline.getLineBuffer
                if (line.trim.length == 0)
                    iterator = Nil.elements
                else
                {
                    val tokens = getTokens(line)
                    val matches = self.completer.complete(token, tokens, line)
                    iterator = matches.elements
                }
            }

            if (iterator.hasNext)
                iterator.next
            else
                null
        }
    }

    private[readline] def doReadline(prompt: String): Option[String] =
    {
        try
        {
            val s = JavaReadline.readline(prompt, /* add to history */ false)
            if (s == null)
                Some("")
            else
                Some(s)
        }

        catch
        {
            case e: java.io.EOFException => None
        }
    }
}

/**
 * JavaReadline implementation of the Readline trait, specialized for the
 * EditLine library.
 */
private[readline] class EditlineImpl(appName: String,
                                     autoAddHistory: Boolean)
    extends JavaReadlineImpl(appName,
                             "Editline",
                             autoAddHistory,
                             JavaReadlineLibrary.Editline)

/**
 * JavaReadline implementation of the Readline trait, specialized for the
 * GNU Readline library.
 */
private[readline] class GNUReadlineImpl(appName: String,
                                        autoAddHistory: Boolean)
    extends JavaReadlineImpl(appName,
                             "GNU Readline",
                             autoAddHistory,
                             JavaReadlineLibrary.GnuReadline)

/**
 * JavaReadline implementation of the Readline trait, specialized for the
 * Getline library.
 */
private[readline] class GetlineImpl(appName: String,
                                        autoAddHistory: Boolean)
    extends JavaReadlineImpl(appName,
                             "Getline",
                             autoAddHistory,
                             JavaReadlineLibrary.Getline)

/*
  ---------------------------------------------------------------------------
  This software is released under a BSD-style license:

  Copyright (c) 2009 Brian M. Clapper. All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

  1.  Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.

  2.  The end-user documentation included with the redistribution, if any,
      must include the following acknowlegement:

        "This product includes software developed by Brian M. Clapper
        (bmc@clapper.org, http://www.clapper.org/bmc/). That software is
        copyright (c) 2009 Brian M. Clapper."

      Alternately, this acknowlegement may appear in the software itself,
      if wherever such third-party acknowlegements normally appear.

  3.  Neither the names "clapper.org", "The Grizzled Scala Library",
      nor any of the names of the project contributors may be used to
      endorse or promote products derived from this software without prior
      written permission. For written permission, please contact
      bmc@clapper.org.

  4.  Products derived from this software may not be called "clapper.org
      Java Utility Library", nor may "clapper.org" appear in their names
      without prior written permission of Brian M. Clapper.

  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
  NO EVENT SHALL BRIAN M. CLAPPER BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

        val result = {for (line <- history) yield line}
        result.toList
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

    object rlCompleter extends JavaReadlineCompleter
    {
        private var iterator: Iterator[String] = null

        def completer(text: String, state: Int): String =
        {
            if (state == 0)
            {
                // First call to completer. Get list of matches.

                val currentLine = JavaReadline.getLineBuffer
                val matches = self.completer.complete(text, currentLine)
                iterator = matches.elements
            }

            if (iterator.hasNext)
            {
                val next = iterator.next
                if (next.startsWith(text))
                    next
                else
                    null
            }
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
{
    /**
     * Subclass-specific initialization.
     */
    override protected def subclassInit(): Unit =
    {
        import grizzled.file.util.joinPath
        import java.io.{File, IOException}

        val initPath = joinPath(System.getProperty("user.home"), ".inputrc")
        val initFile = new File(initPath)
        if (initFile.exists && initFile.isFile)
        {
            try
            {
                JavaReadline.readInitFile(initPath)
            }

            catch
            {
                case e: IOException =>
                    val message = if (e.getMessage != null)
                                      e.getMessage
                                  else
                                      e.getClass.getName
                    println("Warning: Can't load \"" + initPath + "\": " +
                            message)
            }
        }
    }
}

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

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
 * JLine implementation of the traits defined in the base readline package.
 */
package grizzled.readline.jline

import grizzled.readline._
import _root_.jline.{Completor => JLineCompleter, ConsoleReader}

/**
 * History implementation that wraps the JLine history API.
 */
private[jline] class JLineHistory(val reader: ConsoleReader)
    extends History
{
    val history = reader.getHistory
    max = Integer.MAX_VALUE

    /**
     * Get the contents of the history buffer, in a list.
     *
     * @return the history lines
     */
    def get: List[String] =
    {
        import scala.collection.mutable.ArrayBuffer

        val result = new ArrayBuffer[String]
        val it = history.getHistoryList.iterator
        while (it.hasNext)
            result += it.next.asInstanceOf[String]
        result.toList
    }

    /**
     * Clear the history buffer
     */
    def clear = history.clear

    /**
     * Get the last (i.e., most recent) entry from the buffer.
     *
     * @return the most recent entry, as an <tt>Option</tt>, or
     *         <tt>None</tt> if the history buffer is empty
     */
    def last: Option[String] =
    {
        val s = history.current
        if ((s == null) || (s.length == 0)) 
            None 
        else
            Some(s)
    }

    /**
     * Get the current number of entries in the history buffer.
     *
     * @return the size of the history buffer
     */
    def size: Int = history.size

    /**
     * Get maximum history size.
     *
     * @return the current max history size, or 0 for unlimited.
     */
    def max: Int = history.getMaxSize

    /**
     * Set maximum history size.
     *
     * @param newSize the new max history size, or 0 for unlimited.
     */
    def max_=(newSize: Int) = history.setMaxSize(newSize)

    /**
     * Unconditionally appends the specified line to the history.
     *
     * @param line  the line to add
     */
    protected def append(line: String) = history.addToHistory(line)
}

/**
 * JLine implementation of the Readline trait.
 */
private[readline] class JLineImpl(appName: String,
                                  val autoAddHistory: Boolean)
    extends Readline
{
    val name = "JLine"
    val reader = new ConsoleReader
    val history = new JLineHistory(reader)
    reader.addCompletor(jlCompleter)
    reader.setUseHistory(false) // we'll do it manually
    val self = this

    // Need to use a Scala existential type as the parameter to the
    // complete() method, below, because Scala will type the java.util.List
    // parameter as List[_], and will then complain when we try to add a
    // String to the list. The existential type gets around that problem by
    // supplying (by force) type information for the list. See Chapter 29
    // (section 29.3) in the "Programming in Scala" book.
    type JList = java.util.List[T] forSome {type T}
    type JSList = java.util.List[String]

    object jlCompleter extends JLineCompleter
    {
        def complete(buf: String, cursor: Int, completions: JList): Int =
        {
            def save(scalaCompletions: List[String], javaCompletions: JSList) =
            {
                // Hiding this in a method, and casting the incoming
                // java.util.List parameter, keeps Scala's type
                // checker from bitching.
                for (s <- scalaCompletions)
                    javaCompletions.add(s)
            }

            val line = if (buf == null) "" else buf
            // Find the start of the token.
            val i = if (buf == null) 0 else buf.lastIndexOf(' ')
            val tokenStart = if (i >= 0) (i + 1) else 0
            val token = buf.substring(tokenStart)

            val matches = self.completer.complete(token, line)
            save(matches, completions.asInstanceOf[JSList])
            if (completions.size == 0) -1 else tokenStart
        }
    }

    private[readline] def doReadline(prompt: String): Option[String] =
    {
        try
        {
            print(prompt)
            val s = reader.readLine
            if (s == null)
                None
            else
                Some(s)
        }

        catch
        {
            case e: java.io.EOFException => None
        }
    }
}

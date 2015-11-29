/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright © 2009-2016, Brian M. Clapper
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

package grizzled.string

/** Wraps strings on word boundaries to fit within a proscribed output
  * width. The wrapped string may have a prefix or not; prefixes are useful
  * for error messages, for instance. You tell a `WordWrapper` about
  * a prefix by passing a non-empty prefix to the constructor.
  *
  * <h2>Examples:</h2>
  * 
  * {{{Unable to open file /usr/local/etc/wombat: No such file or directory}}}
  *
  * might appear like this without a prefix:
  *
  * {{{
  * Unable to open file /usr/local/etc/wombat: No such file or
  * directory
  * }}}
  *
  * and like this if the prefix is "myprog:"
  *
  * {{{
  * myprog: Unable to open file /usr/local/etc/wombat: No such
  *         file or directory
  * }}}
  *
  * Alternatively, if the output width is shortened, the same message
  * can be made to wrap something like this:
  *
  * {{{
  * myprog: Unable to open file
  *         /usr/local/etc/wombat:
  *         No such file or
  *         directory
  * }}}
  *
  * Note how the wrapping logic will "tab" past the prefix on wrapped
  * lines.
  *
  * This method also supports the notion of an indentation level, which is
  * independent of the prefix. A non-zero indentation level causes each line,
  * including the first line, to be indented that many characters. Thus,
  * initializing a `WordWrapper` object with an indentation value of 4
  * will cause each output line to be preceded by 4 blanks. (It's also
  * possible to change the indentation character from a blank to any other
  * character.
  *
  * <h2>Notes</h2>
  *
  * - The class does not do any special processing of tab characters.
  *   Embedded tab characters can have surprising (and unwanted) effects
  *   on the rendered output.
  * - Wrapping an already wrapped string is an invitation to trouble.
  *
  * @param wrapWidth   the number of characters after which to wrap each line
  * @param indentation how many characters to indent
  * @param prefix      the prefix to use, or "" for none. Cannot be null.
  * @param indentChar  the indentation character to use.
  */
class WordWrapper(val wrapWidth:    Int = 79,
                  val indentation:  Int = 0,
                  val prefix:       String = "",
                  val indentChar:   Char = ' ') {
  require(prefix != null)

  private val prefixLength = prefix.length

  /** Wrap a string, using the wrap width, prefix, indentation and indentation
    * character that were specified to the `WordWrapper` constructor.
    * The resulting string may have embedded newlines in it.
    *
    * @param s  the string to wrap
    *
    * @return the wrapped string
    */
  def wrap(s: String): String = {
    import scala.collection.mutable.ArrayBuffer
    import GrizzledString._

    val indentString = indentChar.toString
    val prefixIndentChars = indentString * prefixLength
    val indentChars = indentString * indentation
    val buf = new ArrayBuffer[String]

    def assembleLine(prefix: String, buf: ArrayBuffer[String]): String =
      prefix + indentChars + buf.mkString(" ")

    def wrapOneLine(line: String, prefix: String): String = {
      val lineOut = new ArrayBuffer[String]
      val result  = new ArrayBuffer[String]
      var localPrefix = prefix

      for (word <- line.split("[\t ]")) {
        val wordLength = word.length

        // Current length is the length of each word in the lineOut
        // buffer, plus a single blank between them, plus the prefix
        // length and indentation length, if any. Use a map operation
        // to map the words to their lengths, and a fold-left operation
        // to sum them up.
        val totalBlanks = lineOut.length - 1
        val wordLengths = (0 /: lineOut.map(_.length)) (_ + _)
        val currentLength = totalBlanks + wordLengths + 
        localPrefix.length + indentation
        if ((wordLength + currentLength + 1) > wrapWidth) {
          result += assembleLine(localPrefix, lineOut)
          lineOut.clear
          localPrefix = prefixIndentChars
        }

        lineOut += word
      }

      if (lineOut.length > 0)
        result += assembleLine(localPrefix, lineOut)

      result.mkString("\n").rtrim
    }

    val lines = s.split("\n")
    buf += wrapOneLine(lines(0), prefix)
    for (line <- lines.drop(1))
      buf += wrapOneLine(line, prefixIndentChars)

    buf mkString "\n"
  }
}

/** Companion object for `WordWrapper`.
  */
object WordWrapper {
  /** Create a `WordWrapper`.
    *
    * @param wrapWidth   the number of characters after which to wrap each line
    * @param indentation how many characters to indent
    * @param prefix      the prefix to use, or "" for none. Cannot be null.
    * @param indentChar  the indentation character to use.
    */
  def apply(wrapWidth:    Int = 79,
            indentation:  Int = 0,
            prefix:       String = "",
            indentChar:   Char = ' ') = {
    new WordWrapper(wrapWidth, indentation, prefix, indentChar)
  }
}

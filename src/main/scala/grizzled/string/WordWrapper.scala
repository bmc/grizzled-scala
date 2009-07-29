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

package grizzled.string

import grizzled.string.implicits._

/**
 * <p>Wraps strings on word boundaries to fit within a proscribed output
 * width. The wrapped string may have a prefix or not; prefixes are useful
 * for error messages, for instance. You tell a <tt>WordWrapper</tt> about
 * a prefix by passing a non-empty prefix to the constructor.</p>
 *
 * <h2>Examples:</h2>
 * 
 * <blockquote><pre>Unable to open file /usr/local/etc/wombat: No such file or directory</pre></blockquote>
 *
 * <p>might appear like this without a prefix:</p>
 *
 * <blockquote><pre>
 * Unable to open file /usr/local/etc/wombat: No such file or
 * directory
 * </pre></blockquote>
 *
 * <p>and like this if the prefix is "myprog:"</p>
 *
 * <blockquote><pre>
 * myprog: Unable to open file /usr/local/etc/wombat: No such
 *         file or directory
 * </pre></blockquote>
 *
 * <p>Alternatively, if the output width is shortened, the same message
 * can be made to wrap something like this:</p>
 *
 * <blockquote><pre>
 * myprog: Unable to open file
 *         /usr/local/etc/wombat:
 *         No such file or
 *         directory
 * </pre></blockquote>
 *
 * <p>Note how the wrapping logic will "tab" past the prefix on wrapped
 * lines.</p>
 *
 * <p>This method also supports the notion of an indentation level, which is
 * independent of the prefix. A non-zero indentation level causes each line,
 * including the first line, to be indented that many characters. Thus,
 * initializing a <tt>WordWrapper</tt> object with an indentation value of 4
 * will cause each output line to be preceded by 4 blanks. (It's also
 * possible to change the indentation character from a blank to any other
 * character.</p>
 *
 * <h2>Notes</h2>
 *
 * <ol>
 *   <li> The class does not do any special processing of tab characters.
 *        Embedded tab characters can have surprising (and unwanted) effects
 *        on the rendered output.
 *   <li> Wrapping an already wrapped string is an invitation to trouble.
 * </ol>
 *
 * @param wrapWidth   the number of characters after which to wrap each line
 * @param indentation how many characters to indent
 * @param prefix      the prefix to use, or "" for none. Cannot be null.
 * @param indentChar  the indentation character to use.
 */
class WordWrapper(val wrapWidth:    Int,
                  val indentation:  Int,
                  val prefix:       String,
                  val indentChar:   Char)
{
    require(prefix != null)

    /**
     * Alternate constructor that allows all parameters to default as follows:
     *
     * <ul>
     *  <li> <tt>wrapWidth=79</tt>
     *  <li> <tt>indentation=0</tt>
     *  <li> <tt>prefix=""</tt>
     *  <li> <tt>indentChar=' '</tt>
     * </ul>
     */ 
    def this() = this(79, 0, "", ' ')

    /**
     * Alternate constructor that specifies only a width, defaulting all
     * other constructor parameters as follows:
     *
     * <ul>
     *  <li> <tt>indentation=0</tt>
     *  <li> <tt>prefix=""</tt>
     *  <li> <tt>indentChar=' '</tt>
     * </ul>
     *
     * @param wrapWidth   the number of characters after which to wrap each line
     */ 
    def this(wrapWidth: Int) = this(wrapWidth, 0, "", ' ')

    /**
     * Alternate constructor that specifies only a width and an indentation
     * level, defaulting all other constructor parameters as follows:
     *
     * <ul>
     *  <li> <tt>prefix=""</tt>
     *  <li> <tt>indentChar=' '</tt>
     * </ul>
     *
     * @param wrapWidth   the number of characters after which to wrap each line
     * @param indentation how many characters to indent
     */ 
    def this(wrapWidth: Int, indentation: Int) = 
        this(wrapWidth, indentation, "", ' ')

    /**
     * Alternate constructor that specifies a width, an indentation level and
     * a prefix, defaulting the <tt>indentChar</tt> parameter to a blank.
     *
     * @param wrapWidth   the number of characters after which to wrap each line
     * @param indentation how many characters to indent
     * @param prefix      the prefix to use, or "" for none. Cannot be null.
     */ 
    def this(wrapWidth: Int, indentation: Int, prefix: String) = 
        this(wrapWidth, indentation, prefix, ' ')

    private val prefixLength = prefix.length

    /**
     * Wrap a string, using the wrap width, prefix, indentation and indentation
     * character that were specified to the <tt>WordWrapper</tt> constructor.
     * The resulting string may have embedded newlines in it.
     *
     * @param s  the string to wrap
     *
     * @return the wrapped string
     */
    def wrap(s: String): String =
    {
        import scala.collection.mutable.ArrayBuffer
        import implicits._

        val indentString = indentChar.toString
        val prefixIndentChars = indentString * prefixLength
        val indentChars = indentString * indentation
        val buf = new ArrayBuffer[String]
        var thePrefix = prefix

        def usePrefix =
        {
            val result = thePrefix
            thePrefix = prefixIndentChars
            result
        }

        def assembleLine(buf: ArrayBuffer[String]): String =
            usePrefix + indentChars + buf.mkString(" ")

        def wrapOneLine(line: String): String =
        {
            val lineOut = new ArrayBuffer[String]
            val result  = new ArrayBuffer[String]
            for (word <- line.split("[\t ]"))
            {
                val wordLength = word.length
                // Current length is the length of each word in the lineOut
                // buffer, plus a single blank between them, plus the prefix
                // length and indentation length, if any. Use a map operation
                // to map the words to their lengths, and a fold-left operation
                // to sum them up.
                val totalBlanks = lineOut.length - 1
                val wordLengths = (0 /: lineOut.map(_.length)) (_ + _)
                val currentLength = totalBlanks + wordLengths + prefixLength +
                                    indentation
                if ((wordLength + currentLength + 1) > wrapWidth)
                {
                    result += assembleLine(lineOut)
                    lineOut.clear
                }

                lineOut += word
            }

            if (lineOut.length > 0)
                result += assembleLine(lineOut)

            result.mkString("\n").rtrim
        }

        for (line <-  s.split("\n"))
            buf += wrapOneLine(line)

        buf mkString "\n"
    }
}

/**
 * Companion object for <tt>WordWrapper</tt>.
 */
object WordWrapper
{
    /**
     * Create a <tt>WordWrapper</tt>.
     *
     * @param wrapWidth   the number of characters after which to wrap each line
     * @param indentation how many characters to indent
     * @param prefix      the prefix to use, or "" for none. Cannot be null.
     * @param indentChar  the indentation character to use.
     */
    def apply(wrapWidth:    Int,
              indentation:  Int,
              prefix:       String,
              indentChar:   Char) =
        new WordWrapper(wrapWidth, indentation, prefix, indentChar)

    /**
     * Create a <tt>WordWrapper</tt> with all the default values.
     */ 
    def apply() = new WordWrapper

    /**
     * Alternate constructor that specifies only a width, defaulting all
     * other constructor parameters.
     *
     * @param wrapWidth   the number of characters after which to wrap each line
     */ 
    def apply(wrapWidth: Int) = new WordWrapper(wrapWidth)

    /**
     * Alternate constructor that specifies only a width and an indentation
     * level, defaulting all other constructor parameters.
     *
     * @param wrapWidth   the number of characters after which to wrap each line
     * @param indentation how many characters to indent
     */ 
    def apply(wrapWidth: Int, indentation: Int) = 
        new WordWrapper(wrapWidth, indentation)

    /**
     * Alternate constructor that specifies a width, an indentation level and
     * a prefix, defaulting the <tt>indentChar</tt> parameter to a blank.
     *
     * @param wrapWidth   the number of characters after which to wrap each line
     * @param indentation how many characters to indent
     * @param prefix      the prefix to use, or "" for none. Cannot be null.
     */ 
    def apply(wrapWidth: Int, indentation: Int, prefix: String) = 
        new WordWrapper(wrapWidth, indentation, prefix)
}

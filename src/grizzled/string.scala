/*---------------------------------------------------------------------------*\
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
\*---------------------------------------------------------------------------*/

package grizzled.string

import scala.util.matching.Regex

import java.io.{File, IOException}

/**
 * An analog to Scala's <tt>RichString</tt> class, providing some methods
 * that neither <tt>RichString</tt> nor <tt>String</tt> provide.
 */
final class GrizzledString(val string: String)
{
    private lazy val LTrimRegex = """^\s*(.*)$""".r

    /**
     * Trim white space from the front (left) of a string.
     *
     * @param s  string to trim
     *
     * @return possibly modified string
     */
    def ltrim: String =
    {
        LTrimRegex.findFirstMatchIn(string) match
        {
            case Some(m) => m.group(1)
            case None    => string
        }
    }

    private lazy val RTrimRegex = """\s*$""".r

    /**
     * Trim white space from the back (right) of a string.
     *
     * @param s  string to trim
     *
     * @return possibly modified string
     */
    def rtrim: String = RTrimRegex.replaceFirstIn(string, "")
}

/**
 * <p>Wraps strings on word boundaries to fit within a proscribed output
 * width. The wrapped string may have a prefix or not; prefixes are useful
 * for error messages, for instance. You tell a <tt>WordWrapper</tt> about
 * a prefix by passing a non-zero prefix length to the constructor.</p>
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
 * Implicits, for conversion from Scala's <tt>RichString</tt> and Java's
 * <tt>String</tt> to <tt>GrizzledString</tt> and back again. Also contains
 * other implicit conversions.
 */
object implicits
{
    import scala.runtime.RichString

    implicit def javaStringToGrizzledString(s: String) = new GrizzledString(s)
    implicit def grizzledStringToJavaString(gs: GrizzledString) = gs.string

    implicit def grizzledStringToRichString(gs: GrizzledString) =
        new RichString(gs.string)
    implicit def richStringToGrizzledString(rs: RichString) =
        new GrizzledString(rs.self)

    /**
     * An implicit conversion that handles creating a Boolean from a string
     * value. This implicit definition, when in scope, allows code like
     * the following:
     *
     * <blockquote><pre>
     * val flag: Boolean = "true" // implicitly converts "true" to <tt>true</tt>
     * </pre></blockquote>
     *
     * This method currently understands the following strings (in any mixture
     * of upper and lower case). It is currently English-specific.
     *
     * <blockquote>true, t, yes, y, 1<br>false, f, no, n, 0</blockquote>
     *
     * @param s  the string to convert
     *
     * @return a boolean value
     *
     * @throws IllegalArgumentException if <tt>s</tt> cannot be parsed
     */
    implicit def bool(s: String): Boolean =
        s.trim.toLowerCase match
        {
            case "true"  => true
            case "t"     => true
            case "yes"   => true
            case "y"     => true
            case "1"     => true

            case "false" => false
            case "f"     => false
            case "no"    => false
            case "n"     => false
            case "0"     => false

            case _       => 
                throw new IllegalArgumentException("Can't convert string \"" +
                                                   s + "\" to boolean.")
        }
}

/**
 * Useful string-related utility functions.
 */
object util
{
    private lazy val QUOTED_REGEX = """(["'])(?:\\?+.)*?\1""".r
    private lazy val WHITE_SPACE_REGEX = """\s+""".r
    private lazy val QUOTE_SET = Set('\'', '"')

    /**
     * <p>Tokenize a string the way a command line shell would, honoring quoted
     * strings and embedded escaped quotes. Single quoted strings must start
     * and end with single quotes. Double quoted strings must start and end
     * with double quotes. Within quoted strings, the quotes themselves may
     * be backslash-escaped. Quoted and non-quoted tokens may be mixed in
     * the string; quotes are stripped.</p>
     *
     * <p>Examples:</p>
     *
     * <blockquote><pre>
     * val s = """one two "three four" ""
     * for (t <- tokenizeWithQuotes(s)) println("|" + t + "|")
     * // Prints:
     * // |one|
     * // |two|
     * // |three four|
     *
     * val s = """one two 'three "four'"""
     * for (t <- tokenizeWithQuotes(s)) println("|" + t + "|")
     * // Prints:
     * // |one|
     * // |two|
     * // |three "four|
     *
     * val s = """one two 'three \'four ' fiv"e"""
     * for (t <- tokenizeWithQuotes(s)) println("|" + t + "|")
     * // Prints:
     * // |one|
     * // |two|
     * // |three 'four |
     * // |fiv"e|
     * </pre></blockquote>
     *
     * @param s  the string to tokenize
     *
     * @return the tokens, as a list of strings
     */
    def tokenizeWithQuotes(s: String): List[String] =
    {
        def fixedQuotedString(qs: String): String =
        {
            val stripped = qs.substring(1, qs.length - 1)
            if (qs(0) == '"')
                stripped.replace("\\\"", "\"")
            else
                stripped.replace("\\'", "'")
        }

        val trimmed = s.trim()

        if (trimmed.length == 0)
            Nil

        else if (QUOTE_SET.contains(trimmed(0)))
        {
            val mOpt = QUOTED_REGEX.findFirstMatchIn(trimmed)
            if (mOpt == None)  // to eol
                List(trimmed)

            else
            {
                val matched = mOpt.get
                val matchedString = matched.toString
                val token = fixedQuotedString(matchedString)
                val past = trimmed.substring(matched.end)
                List(token) ++ tokenizeWithQuotes(past)
            }
        }

        else
        {
            val mOpt = WHITE_SPACE_REGEX.findFirstMatchIn(trimmed)
            if (mOpt == None) // to eol
                List(trimmed)

            else
            {
                val matched = mOpt.get
                val token = trimmed.substring(0, matched.start)
                val past = trimmed.substring(matched.end)
                List(token) ++ tokenizeWithQuotes(past)
            }
        }
    }
}

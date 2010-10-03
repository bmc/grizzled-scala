/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2009-2010, Brian M. Clapper
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

import grizzled.parsing.StringToken

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

/**
 * An analog to Scala's <tt>RichString</tt> class, providing some methods
 * that neither <tt>RichString</tt> nor <tt>String</tt> provide. By
 * importing the implicit conversion functions, you can use the methods in
 * this class transparently from a <tt>String</tt> or <tt>RichString</tt>
 * object.
 *
 * <blockquote><pre>
 * import grizzled.string.implicits._
 *
 * val s = "a  b          c"
 * println(s.tokenize) // prints: List(a, b, c)
 * </pre></blockquote>
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
        LTrimRegex.findFirstMatchIn(string).
                   map(m => m.group(1)).
                   getOrElse(string)
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

    /**
     * Like perl's <tt>chomp()</tt>: Remove any newline at the end of the
     * line.
     *
     * @param line  the line
     *
     * @return the possibly modified line
     */
    def chomp: String =
        if (string.endsWith("\n"))
            string.substring(0, string.length - 1)
        else
            string

    /**
     * Tokenize the string on white space. An empty string and a string
     * with only white space are treated the same. Note that doing a
     * <tt>split("""\s+""").toList</tt> on an empty string ("") yields a
     * list of one item, an empty string. Doing the same operation on a
     * blank string (" ", for example) yields an empty list. This method
     * differs from <tt>split("""\s+""").toList</tt>, in that both cases are
     * treated the same, returning a <tt>Nil</tt>.
     *
     * @return A list of tokens, or <tt>Nil</tt> if there aren't any.
     */
    def tokenize: List[String] =
    {
        string.trim.split("""\s+""").toList match
        {
            case Nil =>
                Nil

            case s :: Nil if (s == "") =>
                Nil

            case s :: Nil =>
                List(s)

            case s :: rest =>
                s :: rest
        }
    }

    /**
     * Tokenize the string on a set of delimiter characters.
     *
     * @param delims the delimiter characters
     *
     * @return A list of tokens, or <tt>Nil</tt> if there aren't any.
     */
    def tokenize(delims: String): List[String] =
    {
        string.trim.split("[" + delims + "]").toList match
        {
            case Nil =>
                Nil

            case s :: Nil =>
                List(s)

            case s :: rest =>
                s :: rest
        }
    }

    /**
     * Tokenize the string on a set of delimiter characters, returning
     * <tt>Token</tt> objects. This method is useful when you need to keep
     * track of the locations of the tokens within the original string.
     *
     * @param delims the delimiter characters
     *
     * @return A list of tokens, or <tt>Nil</tt> if there aren't any.
     */
    def toTokens(delims: String): List[StringToken] =
    {
        val delimRe = ("([^" + delims + "]+)").r

        def find(substr: String, offset: Int): List[StringToken] =
        {
            def handleMatch(m: Match): List[StringToken] =
            {
                val start = m.start
                val end = m.end
                val absStart = start + offset
                val token = StringToken(m.toString, start + offset)
                if (end >= (substr.length - 1))
                    List(token)
                else
                    token :: find(substr.substring(end + 1), end + 1 + offset)
            }

            delimRe.findFirstMatchIn(substr).
                    map(m => handleMatch(m)).
                    getOrElse(Nil)
        }

        find(this.string, 0)
    }

    /**
     * Tokenize the string on white space, returning <tt>Token</tt>
     * objects. This method is useful when you need to keep track of the
     * locations of the tokens within the original string.
     *
     * @return A list of tokens, or <tt>Nil</tt> if there aren't any.
     */
    def toTokens: List[StringToken] = toTokens(""" \t""")

    /**
     * Translate any metacharacters (e.g,. \t, \n, \u2122) into their real
     * characters, and return the translated string. Metacharacter sequences
     * that cannot be parsed (because they're unrecognized, because the Unicode
     * number isn't four digits, etc.) are passed along unchanged.
     *
     * @return the possibly translated string
     */
    def translateMetachars: String =
    {
        // NOTE: Direct matching against Some/None is done here, because it's
        // actually more readable than the (generally preferred) alternatives.

        import grizzled.parsing.{IteratorStream, Pushback}
        import grizzled.string.GrizzledChar._

        val stream = new IteratorStream[Char](string) with Pushback[Char]

        def parseHexDigits: List[Char] =
        {
            stream.next match
            {
                case Some(c) if c.isHexDigit =>
                    c :: parseHexDigits
                case Some(c) =>
                    stream.pushback(c)
                    Nil
                case None =>
                    Nil
            }
        }
    
        def parseUnicode: List[Char] =
        {
            val digits = parseHexDigits
            if (digits == Nil)
                Nil
    
            else if (digits.length != 4)
            {
                // Invalid Unicode string.

                List('\\', 'u') ++ digits
            }
    
            else
                List(Integer.parseInt(digits mkString "", 16).
                     asInstanceOf[Char])
        }
    
        def parseMeta: List[Char] =
        {
            stream.next match
            {
                case Some('t')  => List('\t')
                case Some('f')  => List('\f')
                case Some('n')  => List('\n')
                case Some('r')  => List('\r')
                case Some('\\') => List('\\')
                case Some('u')  => parseUnicode
                case Some(c)    => List('\\', c)
                case None       => Nil
            }
        }
    
        def translate: List[Char] =
        {
            stream.next match
            {
                case Some('\\') => parseMeta ::: translate
                case Some(c)    => c :: translate
                case None       => Nil
            }
        }
    
        translate mkString ""
    }
}

/**
 * Companion object for `GrizzledString`. To get implicit functions that
 * define automatic conversions between `GrizzledString` and `String`,
 * import this module:
 *
 * {{{
 * import grizzled.io.GrizzledString._
 * }}}
 */
object GrizzledString
{
    import scala.collection.immutable.{StringOps, StringLike}

    /**
     * Implicit function to convert from a Java string to a
     * <tt>GrizzledString</tt>.
     *
     * @param s  the Java string
     *
     * @return the <tt>GrizzledString</tt>
     */
    implicit def JavaString_GrizzledString(s: String) = new GrizzledString(s)

    /**
     * Implicit function to convert from a <tt>GrizzledString</tt> to a
     * Java string.
     *
     * @param gs  the <tt>GrizzledString</tt>
     *
     * @return the Java string
     */
    implicit def GrizzledString_JavaString(gs: GrizzledString) = gs.string

    /**
     * Implicit function to convert from a Scala string object to a
     * <tt>GrizzledString</tt>.
     *
     * @param s  a Scala string
     *
     * @return the <tt>GrizzledString</tt>
     */
    implicit def ScalaString_GrizzledString(rs: StringLike[String]) =
        new GrizzledString(rs.toString)

    /**
     * Implicit function to convert from <tt>GrizzledString</tt> to a
     * Scala <tt>RichString</tt>.
     *
     * @param s  the <tt>GrizzledString</tt>
     *
     * @return the Scala string
     */
    implicit def GrizzledString_ScalaString(gs: GrizzledString) =
        new StringOps(gs.string)
}

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

/** String and character implicits.
  */
object Implicits {
  import scala.collection.immutable.StringLike
  import scala.language.implicitConversions
  import grizzled.parsing.StringToken
  import scala.util.matching.Regex.Match

  /** `Char` enrichments
    */
  object Char {
    implicit def GrizzledChar_Char(gc: GrizzledChar): Char = gc.character

    implicit def JavaCharacter_GrizzledChar(c: java.lang.Character): GrizzledChar =
      new GrizzledChar(c.charValue)

    implicit def GrizzledChar_JavaCharacter(c: GrizzledChar): java.lang.Character =
      new java.lang.Character(c.character)

    /** An analog to Scala's `RichChar` class, providing some methods
      * that neither `RichChar` nor `Char` (nor, for that matter,
      * `java.lang.Character`) provide. By importing the implicit conversion
      * functions, you can use the methods in this class transparently from a
      * `Char`, `RichChar` or `Character` object.
      *
      * {{{
      * import grizzled.string.implicits._
      * val ch = 'a'
      * println(ch.isHexDigit) // prints: true
      * }}}
      */
    implicit class GrizzledChar(val character: Char) {
      /** Determine whether the character represents a valid hexadecimal
        * digit. This is a specialization of `isDigit(radix)`.
        *
        * @return `true` if the character is a valid hexadecimal
        *         digit, `false` if not.
        */
      def isHexDigit = isDigit(16)

      /** Determine whether the character represents a valid digit in a
        * given base.
        *
        * @param radix the radix
        * @return `true` if the character is a valid digit in the
        *         indicated radix, `false` if not.
        */
      def isDigit(radix: Int): Boolean = {
        try {
          Integer.parseInt(character.toString, radix)
          true
        }
        catch {
          case _: NumberFormatException => false
        }
      }

      /** Determine if a character is non-printable. Note that the notion
        * of "non-printable" in Unicode can be problematic, depending on the
        * encoding. A printable Unicode character, printed in UTF-8 on a
        * terminal that only handles ISO-8859.1 may not, strictly speaking,
        * be "printable" on that terminal.
        *
        * This method's notion of "printable" assumes that the output device
        * is capable of displaying Unicode encodings (e.g., UTF-8). In other
        * words, this method could also be called `isUnicodePrintable()`.
        *
        * See also http://stackoverflow.com/q/220547
        *
        * @return `true` if printable, `false` if not.
        */
      def isPrintable: Boolean = {
        val block = Option(Character.UnicodeBlock.of(character))
        (!Character.isISOControl(character)) &&
        block.exists(_ != Character.UnicodeBlock.SPECIALS)
      }
    }
  }

  /** String enrichment classes.
    */
  object String {

    implicit def String_GrizzledString(rs: StringLike[String]): GrizzledString =
      new GrizzledString(rs.toString)

    /** An analog to Scala's `RichString` class, providing some methods
      * that neither `RichString` nor `String` provide. By
      * importing the implicit conversion functions, you can use the methods in
      * this class transparently from a `String` or `RichString`
      * object.
      *
      * ===Examples===
      *
      * These examples assume you've included this import:
      *
      * {{{import grizzled.string.Implicits.String._}}}
      *
      * These are just a few of the enrichments available. See below for
      * the entire set.
      *
      * {{{
      * val s = "a  b          c"
      * println(s.tokenize) // prints: List(a, b, c)
      * }}}
      *
      * {{{
      * "    abc   def      ".rtrim // yields "    abc   def"
      * }}}
      *
      * {{{
      * "\u00a9 2016 The Example Company"  // yields "© 2016 The Example Company™
      * }}}
      */
    implicit class GrizzledString(val string: String) {
      private val LTrimRegex = """^\s*(.*)$""".r

      private val SpecialMetachars = Map(
        '\n' -> """\n""",
        '\f' -> """\f""",
        '\t' -> """\t""",
        '\r' -> """\r"""
      )

      /** Trim white space from the front (left) of a string.
        *
        * @return possibly modified string
        */
      def ltrim: String = {
        LTrimRegex.findFirstMatchIn(string).map(m => m.group(1)).getOrElse(string)
      }

      private lazy val RTrimRegex = """\s*$""".r

      /** Trim white space from the back (right) of a string.
        *
        * @return possibly modified string
        */
      def rtrim: String = RTrimRegex.replaceFirstIn(string, "")

      /** Like perl's `chomp()`: Remove any newline at the end of the
        * line.
        *
        * @return the possibly modified line
        */
      def chomp: String =
        if (string.endsWith("\n"))
          string.substring(0, string.length - 1)
        else
          string

      /** Tokenize the string on white space. An empty string and a string
        * with only white space are treated the same. Note that doing a
        * `split("""\s+""").toList` on an empty string ("") yields a
        * list of one item, an empty string. Doing the same operation on a
        * blank string (" ", for example) yields an empty list. This method
        * differs from `split("""\s+""").toList`, in that both cases are
        * treated the same, returning a `Nil`.
        *
        * @return A list of tokens, or `Nil` if there aren't any.
        */
      def tokenize: List[String] = {
        string.trim.split("""\s+""").toList match {
          case Nil                   => Nil
          case s :: Nil if s.isEmpty => Nil
          case s :: Nil              => List(s)
          case s :: rest             => s :: rest
        }
      }

      /** Tokenize the string on a set of delimiter characters.
        *
        * @param delims the delimiter characters
        * @return A list of tokens, or `Nil` if there aren't any.
        */
      def tokenize(delims: String): List[String] = {
        string.trim.split("[" + delims + "]").toList match {
          case Nil       => Nil
          case s :: Nil  => List(s)
          case s :: rest => s :: rest
        }
      }

      /** Tokenize the string on a set of delimiter characters, returning
        * `Token` objects. This method is useful when you need to keep
        * track of the locations of the tokens within the original string.
        *
        * @param delims the delimiter characters
        * @return A list of tokens, or `Nil` if there aren't any.
        */
      def toTokens(delims: String): List[StringToken] = {
        val delimRe = ("([^" + delims + "]+)").r

        def find(substr: String, offset: Int): List[StringToken] = {
          def handleMatch(m: Match): List[StringToken] = {
            val start = m.start
            val end = m.end
            val absStart = start + offset
            val token = StringToken(m.toString, start + offset)
            if (end >= (substr.length - 1))
              List(token)
            else
              token :: find(substr.substring(end + 1), end + 1 + offset)
          }

          delimRe.findFirstMatchIn(substr).map(m => handleMatch(m)).getOrElse(Nil)
        }

        find(this.string, 0)
      }

      /** Tokenize the string on white space, returning `Token` objects. This
        * method is useful when you need to keep track of the locations of
        * the tokens within the original string.
        *
        * @return A list of tokens, or `Nil` if there aren't any.
        */
      def toTokens: List[StringToken] = toTokens(""" \t""")

      /** Escape any non-printable characters by converting them to
        * metacharacter sequences.
        *
        * @return the possibly translated string
        */
      def escapeNonPrintables: String = {
        import Char._

        string.map {
          case c if SpecialMetachars.get(c).isDefined => SpecialMetachars(c)
          case c if c.isPrintable => c
          case c                    => f"\\u${c.toLong}%04x"
        }.mkString("")
      }

      /** Translate any metacharacters (e.g,. \t, \n, \\u2122) into their real
        * characters, and return the translated string. Metacharacter sequences
        * that cannot be parsed (because they're unrecognized, because the
        * Unicode number isn't four digits, etc.) are passed along unchanged.
        *
        * @return the possibly translated string
        */
      def translateMetachars: String = {
        import scala.annotation.tailrec
        import Char._

        def isHexString(s: String): Boolean = s.count(_.isHexDigit) == s.length

        @tailrec
        def doParse(chars: List[Char], buf: String): String = {

          chars match {
            case Nil => buf
            case '\\' :: 't' :: rest  => doParse(rest, buf + "\t")
            case '\\' :: 'n' :: rest  => doParse(rest, buf + "\n")
            case '\\' :: 'r' :: rest  => doParse(rest, buf + "\r")
            case '\\' :: 'f' :: rest  => doParse(rest, buf + "\f")
            case '\\' :: '\\' :: rest => doParse(rest, buf + "\\")

            case '\\' :: 'u' :: a :: b :: c :: d :: rest if isHexString(s"$a$b$c$d") =>
              val chars = Integer.parseInt(Array(a, b, c, d).mkString(""), 16)
              doParse(rest.toList, buf + Character.toChars(chars).mkString(""))

            case '\\' :: 'u' :: rest =>
              doParse(rest, buf + "\\u")

            case '\\' :: c :: rest =>
              doParse(rest, buf + s"\\$c")

            case '\\' :: Nil =>
              buf + "\\"

            case c :: rest => doParse(rest, buf :+ c)
          }
        }

        doParse(this.string.toList, "")
      }
    }
  }
}

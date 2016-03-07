package grizzled.string

import scala.collection.immutable.StringLike

/** String and character implicits.
  */
object Implicits {
  import scala.language.implicitConversions
  import grizzled.parsing.StringToken
  import scala.util.matching.Regex
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
    }
  }

  /** String enrichment classes.
    */
  object String {
    implicit def ScalaString_GrizzledString(rs: StringLike[String]) =
      new GrizzledString(rs.toString)

    /** An analog to Scala's `RichString` class, providing some methods
      * that neither `RichString` nor `String` provide. By
      * importing the implicit conversion functions, you can use the methods in
      * this class transparently from a `String` or `RichString`
      * object.
      *
      * {{{
      * import grizzled.string.Implicits.String._
      *
      * val s = "a  b          c"
      * println(s.tokenize) // prints: List(a, b, c)
      * }}}
      */
    implicit class GrizzledString(val string: String) {
      private lazy val LTrimRegex = """^\s*(.*)$""".r

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
          case s :: Nil if (s == "") => Nil
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

      /** Tokenize the string on white space, returning `Token`
        * objects. This method is useful when you need to keep track of the
        * locations of the tokens within the original string.
        *
        * @return A list of tokens, or `Nil` if there aren't any.
        */
      def toTokens: List[StringToken] = toTokens(""" \t""")

      /** Translate any metacharacters (e.g,. \t, \n, \u2122) into their real
        * characters, and return the translated string. Metacharacter sequences
        * that cannot be parsed (because they're unrecognized, because the Unicode
        * number isn't four digits, etc.) are passed along unchanged.
        *
        * @return the possibly translated string
        */
      def translateMetachars: String = {
        // NOTE: Direct matching against Some/None is done here, because it's
        // actually more readable than the (generally preferred) alternatives.

        import grizzled.parsing.{IteratorStream, Pushback}
        import grizzled.string.Implicits.Char._

        val stream = new IteratorStream[Char](string) with Pushback[Char]

        def parseHexDigits: List[Char] = {
          stream.next match {
            case Some(c) if c.isHexDigit => c :: parseHexDigits
            case Some(c)                 => stream.pushback(c); Nil
            case None                    => Nil
          }
        }

        def parseUnicode: List[Char] = {
          val digits = parseHexDigits
          if (digits == Nil)
            Nil

          else if (digits.length != 4) {
            // Invalid Unicode string.

            List('\\', 'u') ++ digits
          }

          else
            List(Integer.parseInt(digits mkString "", 16).asInstanceOf[Char])
        }

        def parseMeta: List[Char] = {
          stream.next match {
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

        def translate: List[Char] = {
          stream.next match {
            case Some('\\') => parseMeta ::: translate
            case Some(c)    => c :: translate
            case None       => Nil
          }
        }

        translate mkString ""
      }
    }
  }
}

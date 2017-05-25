package grizzled.string

import scala.annotation.tailrec
import scala.util.Try

/** Useful string-related utility functions.
  */
object util {

  private val BooleanStrings = Map(
    "true"  -> true,
    "t"     -> true,
    "yes"   -> true,
    "y"     -> true,
    "1"     -> true,
    "on"    -> true,
    "false" -> false,
    "f"     -> false,
    "no"    -> false,
    "n"     -> false,
    "0"     -> false,
    "off"   -> false
  )

  /** Convert a string to a boolean.
    *
    * This method currently understands the following strings (in any mixture
    * of upper and lower case). It is currently English-specific.
    *
    * {{{
    * true, t, yes, y, 1
    * false, f, no, n, 0
    * }}}
    *
    * @param s  the string to convert
    *
    * @return `Right(boolean)` on success, `Left(error)` on failure
    */
  def strToBoolean(s: String): Either[String, Boolean] = {
    BooleanStrings.get(s.trim.toLowerCase)
      .map(Right(_))
      .getOrElse(Left(s"Cannot convert '$s' to boolean"))
  }

  /** Convert an array of bytes to a hexadecimal string.
    *
    * @param bytes the array of bytes
    *
    * @return the hexadecimal string, with lower-case hex digits and no
    *         separators.
    */
  def bytesToHexString(bytes: Array[Byte]): String = {
    bytes.map("%02x" format _).mkString
  }

  // Presumably faster than Integer.parseInt()
  private val HexDigits = Map(
    '0' -> 0,  '1' -> 1,  '2' -> 2,  '3' -> 3,
    '4' -> 4,  '5' -> 5,  '6' -> 6,  '7' -> 7,
    '8' -> 8,  '9' -> 9,  'a' -> 10, 'b' -> 11,
    'c' -> 12, 'd' -> 13, 'e' -> 14, 'f' -> 15
  )

  /** Convert a hex string to bytes.
    *
    * @param hexString  the hex string
    *
    * @return `Some(bytes)` if the string was succesfully parsed;
    *         `None` if the string could not be parsed.
    */
  def hexStringToBytes(hexString: String): Option[Array[Byte]] = {

    @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.AsInstanceOf"))
    def parse(chars: Seq[Char], accum: Array[Byte]): Option[Array[Byte]] = {
      chars match {
        case upper :: lower :: rest => {
          val res = for { u <- HexDigits.get(upper)
                          l <- HexDigits.get(lower) }
                    yield ((u << 4) | l).asInstanceOf[Byte]

          res map { byte => parse(rest, accum :+ byte) } getOrElse { None }
        }
        case Nil => Some(accum)
      }
    }

    if ((hexString.length % 2) == 0)
      parse(hexString.toLowerCase.toList, Array.empty[Byte])
    else
      None
  }

  private lazy val QUOTED_REGEX = """(["'])(?:\\?+.)*?\1""".r
  private lazy val WHITE_SPACE_REGEX = """\s+""".r
  private lazy val QUOTE_SET = Set('\'', '"')

  /** Tokenize a string the way a command line shell would, honoring quoted
    * strings and embedded escaped quotes. Single quoted strings must start
    * and end with single quotes. Double quoted strings must start and end
    * with double quotes. Within quoted strings, the quotes themselves may
    * be backslash-escaped. Quoted and non-quoted tokens may be mixed in
    * the string; quotes are stripped.
    *
    * Examples:
    *
    * {{{
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
    * }}}
    *
    * @param s  the string to tokenize
    *
    * @return the tokens, as a list of strings
    */
  def tokenizeWithQuotes(s: String): List[String] = {
    def fixedQuotedString(qs: String): String = {
      val stripped = qs.substring(1, qs.length - 1)
      if (qs(0) == '"')
        stripped.replace("\\\"", "\"")
      else
        stripped.replace("\\'", "'")
    }

    val trimmed = s.trim()

    if (trimmed.length == 0)
      Nil

    else if (QUOTE_SET.contains(trimmed(0))) {
      val mOpt = QUOTED_REGEX.findFirstMatchIn(trimmed)
      mOpt.map { matched =>
        val matchedString = matched.toString
        val token = fixedQuotedString(matchedString)
        val past = trimmed.substring(matched.end)
        List(token) ++ tokenizeWithQuotes(past)
      }
      .getOrElse( // to EOL
        List(trimmed)
      )
    }

    else {
      val mOpt = WHITE_SPACE_REGEX.findFirstMatchIn(trimmed)
      mOpt.map { matched =>
        val token = trimmed.substring(0, matched.start)
        val past = trimmed.substring(matched.end)
        List(token) ++ tokenizeWithQuotes(past)
      }
      .getOrElse( // to EOL
        List(trimmed)
      )
    }
  }

  /** Given a sequence of strings, find the longest common prefix.
    *
    * @param strings  the strings to compare
    *
    * @return the longest common prefix, which might be ""
    */
  def longestCommonPrefix(strings: Seq[String]): String = {

    def isCommonPrefix(len: Int): Boolean = {
      val prefix = strings.head.slice(0, len)

      // If there's a single string in the list that doesn't start with this
      // prefix, the first N characters of strings(0) is not a common prefix.
      ! strings.exists { s => ! s.startsWith(prefix) }
    }

    @tailrec
    def search(low: Int, high: Int): (Int, Int) = {
      if (low > high)
        (low, high)
      else {
        val middle = (low + high) / 2
        if (isCommonPrefix(middle))
          search(low = middle + 1, high = high)
        else
          search(low = low, high = middle - 1)
      }

    }

    if (strings.isEmpty) {
      ""
    }

    else {
      val minLen = strings.map(_.length).min
      val (low, high) = search(1, minLen)
      strings.head.slice(0, (low + high) / 2)
    }
  }

}

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
      if (mOpt == None)  // to eol
        List(trimmed)

      else {
        val matched = mOpt.get
        val matchedString = matched.toString
        val token = fixedQuotedString(matchedString)
        val past = trimmed.substring(matched.end)
        List(token) ++ tokenizeWithQuotes(past)
      }
    }

    else {
      val mOpt = WHITE_SPACE_REGEX.findFirstMatchIn(trimmed)
      if (mOpt == None) // to eol
        List(trimmed)

      else {
        val matched = mOpt.get
        val token = trimmed.substring(0, matched.start)
        val past = trimmed.substring(matched.end)
        List(token) ++ tokenizeWithQuotes(past)
      }
    }
  }
}

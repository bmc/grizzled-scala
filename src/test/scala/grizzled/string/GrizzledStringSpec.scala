/*
  ---------------------------------------------------------------------------
  Copyright © 2009-2018, Brian M. Clapper. All rights reserved.

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

import grizzled.BaseSpec
/**
 * Tests the GrizzledString class.
 */
class GrizzledStringSpec extends BaseSpec {
  import grizzled.string.Implicits.String._

  "ltrim" should "properly trim from the beginning of a string" in {
    val data = Map(
      "a b c"                        -> "a b c",
      "                     a"       -> "a",
      "                     a  "     -> "a  ",
      "                      "       -> "",
      ""                             -> ""
    )

    for((input, expected) <- data) {
      input.ltrim shouldBe expected
    }
  }

  "rtrim" should "properly trim from the end of a string" in {

    val data = Map(
      "a b c"                        -> "a b c",
      "a                     "       -> "a",
      "  a                     "     -> "  a",
      "                      "       -> "",
      ""                             -> ""
    )

    for((input, expected) <- data) {
      input.rtrim shouldBe expected
    }
  }

  "tokenize" should "properly break a line into tokens" in {
    val data = Map(
      ""                       -> Nil,
      " "                      -> Nil,
      "      "                 -> Nil,
      "\t  "                   -> Nil,
      "   a b    c"            -> List("a", "b", "c"),
      "one  two   three four " -> List("one", "two", "three", "four")
    )

    for((input, expected) <- data) {
      input.tokenize shouldBe expected
    }
  }

  "translateMetachars" should "translate metacharacter sequences into chars" in {
    val data = Array(
      "a b c"            -> "a b c",
      "\\t\\n\\afooness" -> "\t\n\\afooness",
      "\\u2122"          -> "\u2122",
      "\\u212a"          -> "\u212a",
      "\\u212x"          -> "\\u212x",
      "\\\\t"            -> "\\t"
    )

    for ((input, expected) <- data) {
      input.translateMetachars shouldBe expected
    }
  }

  it should "handle embedded metacharacter sequences" in {
    val data = Array(
      "\\u0160ablonas"                    -> "\u0160ablonas",
      "\\u015eablon tart\\u0131\\u015fma" -> "\u015eablon tart\u0131\u015fma",
      "Tart\\u0131\\u015fma"              -> "Tart\u0131\u015fma",
      "Tart\\u0131\\u015fma\\nabc"        -> "Tart\u0131\u015fma\nabc"
    )

    for ((input, expected) <- data) {
      input.translateMetachars shouldBe expected
    }
  }

  "escapeNonPrintables" should "return an entirely printable string as is" in {
    val s = "\u2122This is a \u00a9 string"
    s.escapeNonPrintables should be (s)
  }

  it should "escape ISO non-printables properly" in {
    val iso = (0x7f to 0xff).map(_.toChar).filter{c => Character.isISOControl(c)}
    iso should not be empty

    iso.mkString("").escapeNonPrintables should be (
      "\\u007f\\u0080\\u0081\\u0082\\u0083\\u0084\\u0085\\u0086\\u0087\\u0088" +
      "\\u0089\\u008a\\u008b\\u008c\\u008d\\u008e\\u008f\\u0090\\u0091\\u0092" +
      "\\u0093\\u0094\\u0095\\u0096\\u0097\\u0098\\u0099\\u009a\\u009b\\u009c" +
      "\\u009d\\u009e\\u009f"
    )
  }

  it should "handle special metacharacters" in  {
    "\n\r\t\f".escapeNonPrintables should be ("""\n\r\t\f""")
  }

  "replaceFirstChar" should "replace properly, using a replacment char " in {
    "abcdefghij".replaceFirstChar('a', 'X') shouldBe "Xbcdefghij"
  }

  it should "replace properly, using a replacement string" in {
    "asdlkfj".replaceFirstChar('a', "ZZZ") shouldBe "ZZZsdlkfj"
  }

  it should "only replace the first instance" in {
    "aaaaaaaaaa".replaceFirstChar('a', 'A') shouldBe "Aaaaaaaaaa"
  }

  it should "ignore any regular expression characters" in {
    val source = "a.b"
    source.replaceFirst(".", "X") shouldBe "X.b"
    source.replaceFirstChar('.', 'X') shouldBe "aXb"
  }
}

/*
 ---------------------------------------------------------------------------
  Copyright © 2009-2016, Brian M. Clapper. All rights reserved.

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
  --------------------------------------------------------------------------
*/

import org.scalatest.{FlatSpec, Matchers}
import grizzled.string._
import grizzled.string.util._

/**
 * Tests the grizzled.string functions.
 */
class StringUtilSpec extends FlatSpec with Matchers {
  "strToBoolean" should "succeed on valid input" in {
    val data = Map(
      "true"  -> Right(true),
      "t"     -> Right(true),
      "yes"   -> Right(true),
      "y"     -> Right(true),
      "1"     -> Right(true),

      "false" -> Right(false),
      "f"     -> Right(false),
      "no"    -> Right(false),
      "n"     -> Right(false),
      "0"     -> Right(false)
    )

    for {(input, expected) <- data;
         permutations = List(input,
                             input.capitalize,
                             input.toUpperCase,
                             " " + input,
                             " " + input + " ",
                             input + " ")
         s <- permutations} {

      util.strToBoolean(s) shouldBe expected
    }
  }

  it should "fail on invalid input" in {
    val data = List("tru", "tr", "z", "truee", "xtrue",
                    "000", "00", "111", "1a", "0z",
                    "fa", "fal", "fals", "falsee")

    for {input <- data
         permutations = List(input, input.capitalize, input.toUpperCase)
         s <- permutations} {
      util.strToBoolean(s).isLeft shouldBe true
    }
  }

  "tokenizeWithQuotes" should "handle quoted strings" in {
    val data = Map(
      "a b c"                        -> List("a", "b", "c"),
      "aa bb cc"                     -> List("aa", "bb", "cc"),
      "\"aa\\\"a\" 'b'"              -> List("aa\"a", "b"),
      "one two '3\" four'"       -> List("one", "two", "3\" four"),
      "\"a'b    c'\" 'b\\'c  d' a\"" -> List("a'b    c'", "b'c  d", "a\"")
    )

    for((input, expected) <- data) {
      tokenizeWithQuotes(input) shouldBe expected
    }
  }

  "bytesToHexString" should "produce proper hex strings" in {
    val Data = Seq(
      byteArray(Array(0x10, 0x13, 0x99, 0xff)) -> "101399ff"
    )

    for ((bytes, expected) <- Data) {

      bytesToHexString(bytes) shouldBe expected
    }
  }

  "hexStringToBytes" should "properly decode valid hex strings" in {
    val Data = Seq(
      "101399ff" -> Some(byteArray(Array(0x10, 0x13, 0x99, 0xff))),
      "fail"     -> None,
      "FFBC9D"   -> Some(byteArray(Array(0xff, 0xbc, 0x9d)))
    )

    def eqByteArray(b1: Array[Byte], b2: Array[Byte]): Boolean = {
      val s1 = b1.toSet
      val s2 = b2.toSet
      s2 == s1
    }

    def eqOpt(o1: Option[Array[Byte]], o2: Option[Array[Byte]]): Boolean = {
      o1.map { b1 => o2.isDefined && eqByteArray(b1, o2.get) }
        .getOrElse( o2.isEmpty )
    }

    for ((s, byteOpt) <- Data) {
      eqOpt(byteOpt, hexStringToBytes(s)) shouldBe true
    }
  }

  private def byteArray(b: Array[Int]) = b.map { _.asInstanceOf[Byte] }

}

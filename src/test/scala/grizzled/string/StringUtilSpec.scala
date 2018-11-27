package grizzled.string

import grizzled.BaseSpec
import grizzled.string._
import grizzled.string.util._

/**
 * Tests the grizzled.string functions.
 */
class StringUtilSpec extends BaseSpec {
  "strToBoolean" should "succeed on valid input" in {
    // Type annotations on Map are for IntelliJ, which gets confused...
    val data = Map[String, Either[String, Boolean]](
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

    for {(input: String, expected: Either[String, Boolean]) <- data
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

  "longestCommonPrefix" should "properly find a common prefix" in {
    longestCommonPrefix(Seq("abc", "abcdef", "abcdefg")) shouldBe "abc"
    longestCommonPrefix(Seq("a", "abcdef", "abcdefg")) shouldBe "a"
    longestCommonPrefix(Seq("ab", "abcdef", "abcdefg")) shouldBe "ab"
  }

  it should "properly handle an array of length 1" in {
    longestCommonPrefix(Seq("a")) shouldBe "a"
  }

  it should "properly handle an array of length 0" in {
    longestCommonPrefix(Seq.empty[String]) shouldBe ""
  }

  it should "properly handle an array containing N of the same string" in {
    val a = (1 to 20).map(_ => "xxx")
    longestCommonPrefix(a) shouldBe "xxx"
  }

  it should "properly handle an array with an empty string" in {
    longestCommonPrefix(Seq("abc", "abcdef", "abcdefg", "")) shouldBe ""
  }

  it should "properly handle an array with no common substring" in {
    longestCommonPrefix(Seq("abc", "abcdef", "abcdefg", "xyz")) shouldBe ""
  }

  private def byteArray(b: Array[Int]) = b.map { _.asInstanceOf[Byte] }

}

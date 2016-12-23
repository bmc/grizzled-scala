package grizzled.random

import grizzled.BaseSpec

import scala.collection.BitSet
import scala.util.Random

/** For testing random helpers.
  */
class RandomSpec extends BaseSpec {
  val Iterations = 100

  "randomChoice" should "return a random value from an array of integers" in {
    val a = Random.shuffle(1 to 200).toArray
    for (_ <- 1 to Iterations) {
      val n = RandomUtil.randomChoice(a)
      a should contain (n)
    }
  }

  it should "return a random value from an array of strings" in {
    val a = (1 to 200).map { _ => Random.nextString(20) }.toArray
    for (_ <- 1 to Iterations) {
      val s = RandomUtil.randomChoice(a)
      a should contain (s)
    }
  }

  it should "work on a Vector" in {
    val a = (1 to 200).map { _ => Random.nextString(20) }.toVector
    for (_ <- 1 to Iterations) {
      val s = RandomUtil.randomChoice(a)
      a should contain (s)
    }
  }

  it should "return the same elements if a constant seed is used" in {
    val seed = Random.nextInt
    val ru1 = new RandomUtil(new Random(seed))
    val ru2 = new RandomUtil(new Random(seed))
    val a = Random.shuffle(1 to 10000).toArray

    val seq1 = (1 to 20).map { _ => ru1.randomChoice(a) }
    val seq2 = (1 to 20).map { _ => ru2.randomChoice(a) }

    seq1 shouldBe seq2
  }

  "randomIntBetween" should "always return an integer between lower and upper" in {
    val lower = 1
    val upper = 100
    val nums  = BitSet(1 to 100: _*)

    for (i <- 1 to Iterations) {
      val n = RandomUtil.randomIntBetween(1, 100)
      nums should contain (n)
    }
  }

  it should "abort if lower is less than upper" in {
    an [IllegalArgumentException] should be thrownBy
      RandomUtil.randomIntBetween(100, 1)
  }

  it should "always return the same number if low == high" in {
    for (i <- 1 to Iterations) {
      val n = RandomUtil.randomIntBetween(100, 100)
      n shouldBe 100
    }
  }

  it should "return the same elements if a constant seed is used" in {
    val seed = Random.nextInt
    val low = 1
    val high = 10000
    val ru1 = new RandomUtil(new Random(seed))
    val ru2 = new RandomUtil(new Random(seed))

    val seq1 = (1 to 200).map { _ => ru1.randomIntBetween(low, high) }
    val seq2 = (1 to 200).map { _ => ru2.randomIntBetween(low, high) }

    seq1 shouldBe seq2
  }

  "randomString" should "return a random alphanumeric of the right length" in {
    val chars = RandomUtil.DefaultRandomStringChars.toSet
    for (len <- 1 to 100) {
      for (_ <- 1 to Iterations) {
        val s = RandomUtil.randomString(len)
        s.length shouldBe len
        (s.toSet | chars) shouldBe chars
      }
    }
  }

  it should "return a string composed of specific characters" in {
    val chars = Random.nextString(30)
    val charSet = chars.toSet
    for (len <- 1 to 100) {
      for (_ <- 1 to Iterations) {
        val s = RandomUtil.randomString(len, chars)
        s.length shouldBe len
        (s.toSet | charSet) shouldBe charSet
      }
    }
  }

  it should "return the same sequence of strings if a constant seed is used" in {
    val seed = Random.nextInt
    val ru1 = new RandomUtil(new Random(seed))
    val ru2 = new RandomUtil(new Random(seed))
    val len = 100

    val seq1 = (1 to 200).map { _ => ru1.randomString(len) }
    val seq2 = (1 to 200).map { _ => ru2.randomString(len) }

    seq1 shouldBe seq2
  }

  it should "fail if the string of legal characters is empty" in {
    an [IllegalArgumentException] should be thrownBy
      RandomUtil.randomString(10, "")
  }

  it should "always return the same string if only one legal character is used" in {
    for (len <- 10 to 30) {
      val expected = "A" * len
      val set = (1 to 30).map { _ => RandomUtil.randomString(len, "A") }.toSet
      set.size shouldBe 1
    }
  }
}

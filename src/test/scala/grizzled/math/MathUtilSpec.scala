package grizzled.math

import grizzled.BaseSpec
import grizzled.math.util.{max => maximum, min => minimum}

import scala.util.Random

class MathUtilSpec extends BaseSpec {
  "max" should "work on floats" in {
    import grizzled.ScalaCompat.math.Ordering.Float.IeeeOrdering
    val nums = (1 to 100).map(_ => Random.nextFloat * 1000)
    val biggest = nums.sortWith { _ > _ }.head
    maximum(nums.head, nums.tail: _*) shouldBe biggest
  }

  it should "work on integers" in {
    val nums = (1 to 100).map(_ => Random.nextInt(1000))
    val biggest = nums.sortWith { _ > _ }.head
    maximum(nums.head, nums.tail: _*) shouldBe biggest
  }

  it should "work on bytes" in {
    val bytes = new Array[Byte](1000)
    Random.nextBytes(bytes)
    val biggest = bytes.sortWith { _ > _ }.head
    maximum(bytes.head, bytes.tail.toSeq: _*) shouldBe biggest
  }

  it should "work on doubles" in {
    import grizzled.ScalaCompat.math.Ordering.Double.IeeeOrdering
    val nums = (1 to 100).map(_ => Random.nextDouble * 100000000)
    val biggest = nums.sortWith { _ > _ }.head
    maximum(nums.head, nums.tail: _*) shouldBe biggest
  }

  it should "work on BigInts" in {
    val nums = (1 to 100).map(_ => BigInt(Random.nextLong))
    val biggest = nums.sortWith { _ > _ }.head
    maximum(nums.head, nums.tail: _*) shouldBe biggest
  }

  it should "work on BigDecimals" in {
    val nums = (1 to 100).map(_ => BigDecimal(Random.nextDouble * 100000000))
    val biggest = nums.sortWith { _ > _ }.head
    maximum(nums.head, nums.tail: _*) shouldBe biggest
  }

  it should "work on Strings" in {
    val strings = Array(
      "klasdfj",
      "8haklshdfasdjh asdkjhasdfh",
      "jklasdf lkjasdf",
      "wertyuijkl"
    )
    val biggest = strings.sortWith { _ > _ }.head
    maximum(strings.head, strings.tail.toSeq: _*) shouldBe biggest
  }

  "min" should "work on floats" in {
    import grizzled.ScalaCompat.math.Ordering.Float.IeeeOrdering
    val nums = (1 to 100).map(_ => Random.nextFloat * 1000)
    val biggest = nums.sortWith { _ < _ }.head
    minimum(nums.head, nums.tail: _*) shouldBe biggest
  }

  it should "work on integers" in {
    val nums = (1 to 100).map(_ => Random.nextInt(1000))
    val biggest = nums.sortWith { _ < _ }.head
    minimum(nums.head, nums.tail: _*) shouldBe biggest
  }

  it should "work on bytes" in {
    val bytes = new Array[Byte](1000)
    Random.nextBytes(bytes)
    val biggest = bytes.sortWith { _ < _ }.head
    minimum(bytes.head, bytes.tail.toSeq: _*) shouldBe biggest
  }

  it should "work on doubles" in {
    import grizzled.ScalaCompat.math.Ordering.Double.IeeeOrdering
    val nums = (1 to 100).map(_ => Random.nextDouble * 100000000)
    val biggest = nums.sortWith { _ < _ }.head
    minimum(nums.head, nums.tail: _*) shouldBe biggest
  }

  it should "work on BigInts" in {
    val nums = (1 to 100).map(_ => BigInt(Random.nextLong))
    val biggest = nums.sortWith { _ < _ }.head
    minimum(nums.head, nums.tail: _*) shouldBe biggest
  }

  it should "work on BigDecimals" in {
    val nums = (1 to 100).map(_ => BigDecimal(Random.nextDouble * 100000000))
    val biggest = nums.sortWith { _ < _ }.head
    minimum(nums.head, nums.tail: _*) shouldBe biggest
  }

  it should "work on Strings" in {
    val strings = Seq(
      "klasdfj",
      "8haklshdfasdjh asdkjhasdfh",
      "jklasdf lkjasdf",
      "wertyuijkl"
    )
    val biggest = strings.sortWith { _ < _ }.head
    minimum(strings.head, strings.tail: _*) shouldBe biggest
  }
}

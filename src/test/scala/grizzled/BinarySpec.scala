
package grizzled

import grizzled.binary._

/**
 * Tests the grizzled.binary functions.
 */
class BinarySpec extends BaseSpec {
  "bitCount" should "properly count bits" in {
    val intData = Map[Int, Int](
      0                -> 0,
      1                -> 1,
      2                -> 1,
      3                -> 2,
      0x44444444       -> 8,
      0xeeeeeeee       -> 24,
      0xffffffff       -> 32,
      0x7fffffff       -> 31
    )

    val longData = Map[Long, Int](
      0L                   -> 0,
      1L                   -> 1,
      2L                   -> 1,
      3L                   -> 2,
      0x444444444L         -> 9,
      0xeeeeeeeeeL         -> 27,
      0xffffffffL          -> 32,
      0x7fffffffL          -> 31,
      0xffffffffffffL      -> 48
    )

    for((n, expected) <- intData) {
      bitCount(n) shouldBe expected
    }

    for((n, expected) <- longData) {
      bitCount(n) shouldBe expected
    }
  }
}

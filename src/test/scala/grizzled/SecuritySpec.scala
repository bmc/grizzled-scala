package grizzled

import grizzled.security._
import scala.io.Source
import java.io.ByteArrayInputStream

/**
 * Tests the grizzled.security functions.
 */
class SecuritySpec extends BaseSpec {
  val Data = Array(
    ("sha-256", "foo")  -> "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae",
    ("md5", "foo")      -> "acbd18db4cc2f85cedef654fccc4a4d8",
    ("md5", "ab")       -> "187ef4436122d1cc2f40dc2b92f0eba0",
    ("md5", "a\u0000b") -> "70350f6027bce3713f6b76473084309b"
  )

  "MessageDigest" should "work with string inputs" in {

    for (((algorithm, str), expected) <- Data) {
      MessageDigest(algorithm).digestString(str) shouldBe expected
    }
  }

  it should "work with Source inputs" in {
    for (((algorithm, str), expected) <- Data) {
      MessageDigest(algorithm).digestString(Source.fromString(str)) shouldBe expected
    }
  }

  it should "work with InputStream inputs" in {
    for (((algorithm, str), expected) <- Data) {
      val stream = new ByteArrayInputStream(str.getBytes)
      MessageDigest(algorithm).digestString(stream) shouldBe expected
    }
  }
}

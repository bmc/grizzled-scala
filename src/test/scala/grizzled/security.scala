
import org.scalatest.FunSuite
import grizzled.security._
import scala.io.Source
import java.io.ByteArrayInputStream

/**
 * Tests the grizzled.security functions.
 */
class SecurityTest extends FunSuite {
  val Data = Array(
    ("sha-256", "foo") -> "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae",
    ("md5", "foo")     -> "acbd18db4cc2f85cedef654fccc4a4d8"
  )

  test("MessageDigest: string inputs") {

    for (testItem <- Data) {
      val (algorithm, str) = testItem._1
      val expected         = testItem._2

      expectResult(expected) {
        MessageDigest(algorithm).digestString(str)
      }
    }
  }

  test("MessageDigest: source inputs") {

    for (testItem <- Data) {
      val (algorithm, str) = testItem._1
      val expected         = testItem._2

      expectResult(expected) {
        MessageDigest(algorithm).digestString(Source.fromString(str))
      }
    }
  }

  test("MessageDigest: InputStream inputs") {

    for (testItem <- Data) {
      val (algorithm, str) = testItem._1
      val expected         = testItem._2

      expectResult(expected) {
        val stream = new ByteArrayInputStream(str.getBytes)
        MessageDigest(algorithm).digestString(stream)
      }
    }
  }
}

package grizzled.util

import org.scalatest.{FlatSpec, Matchers}

import scala.util.Try

class withResourceSpec extends FlatSpec with Matchers {
  private class TestCloseable extends java.io.Closeable {
    var isClosed = false
    def close() = isClosed = true
  }

  "withResource" should "close a java.io.Closeable on success" in {
    val c = new TestCloseable
    withResource(c) { _ =>  }
    c.isClosed shouldBe true
  }

  it should "close a java.io.Closeable on failure" in {
    val c = new TestCloseable
    Try {
      withResource(c) { _ => throw new Exception("abort") }
    }

    c.isClosed shouldBe true
  }
}

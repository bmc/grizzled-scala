package grizzled.util

import grizzled.BaseSpec

import scala.util.Try

class WithResourceSpec extends BaseSpec {
  private class TestCloseable extends java.io.Closeable with AutoCloseable {
    var isClosed = false
    def close(): Unit = isClosed = true
  }

  "withResource" should "close a java.io.Closeable on success" in {
    import grizzled.util.CanReleaseResource.Implicits.CanReleaseCloseable

    val c = new TestCloseable
    withResource(c) { _ =>  }
    c.isClosed shouldBe true
  }

  it should "close a java.io.Closeable on failure" in {
    import grizzled.util.CanReleaseResource.Implicits.CanReleaseCloseable

    val c = new TestCloseable
    Try {
      withResource(c) { _ => throw new Exception("abort") }
    }

    c.isClosed shouldBe true
  }

  it should "propagate a thrown exception" in {
    import grizzled.util.CanReleaseResource.Implicits.CanReleaseCloseable

    intercept[java.io.IOException] {
      withResource(new TestCloseable) { _ => throw new java.io.IOException("") }
    }
  }

  "tryWithResource" should "return a Success on success" in {
    import grizzled.util.CanReleaseResource.Implicits.CanReleaseCloseable

    val t = tryWithResource(new TestCloseable) { _ => 1 }
    t shouldBe 'success
    t.get shouldBe 1
  }

  it should "close a java.io.Closeable on success" in {
    import grizzled.util.CanReleaseResource.Implicits.CanReleaseCloseable

    val c = new TestCloseable
    val t = tryWithResource(c) { _ => 1 }
    t shouldBe 'success
    c.isClosed shouldBe true
  }

  it should "close a java.lang.AutoCloseable on success" in {
    import grizzled.util.CanReleaseResource.Implicits.CanReleaseAutoCloseable

    val c = new TestCloseable
    val t = tryWithResource(c) { _ => true }

    t shouldBe 'success
    c.isClosed shouldBe true
  }

  it should "return a Failure when an exception is thrown" in {
    import grizzled.util.CanReleaseResource.Implicits.CanReleaseCloseable

    val c = new TestCloseable
    val f = tryWithResource(c) { _ =>
      throw new java.io.IOException("oops")
    }

    f shouldBe 'failure

    val msg = f.recover {
      case e: Exception => e.getMessage
    }
    msg shouldBe 'success
    msg.get shouldBe "oops"
  }

  it should "close a java.io.Closeable on failure" in {
    import grizzled.util.CanReleaseResource.Implicits.CanReleaseCloseable

    val c = new TestCloseable
    tryWithResource(c) { _ => throw new java.io.IOException("") }
    c.isClosed shouldBe true
  }
}

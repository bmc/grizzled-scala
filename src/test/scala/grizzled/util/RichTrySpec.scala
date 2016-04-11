package grizzled.util

import java.io.IOException

import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Failure, Success}

class RichTrySpec extends FlatSpec with Matchers {
  import grizzled.util.Implicits.RichTry

  "toFuture" should "convert a successful Try into a succeeded Future" in {
    val fut = Success(10).toFuture
    fut.isCompleted shouldBe true
    fut.value shouldBe Some(Success(10))
  }

  it should "convert a failed Try into a failed Future" in {
    val fut = Failure(new IOException("Failed")).toFuture
    fut.isCompleted shouldBe true
    fut.value shouldBe defined
    intercept[IOException] { fut.value.get.get }
  }
}

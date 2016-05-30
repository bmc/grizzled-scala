package grizzled

import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.matchers.{BeMatcher, MatchResult, Matcher}

import scala.util.Try

/** Custom matchers to make the tests easier to read, especially when they
  * fail. See http://doc.scalatest.org/2.2.6/#org.scalatest.matchers.BeMatcher
  */
trait CustomMatchers {
  import Matchers._

  /** Match against a scala.util.Try.
    */
  class SuccessMatcher extends BeMatcher[Try[_]] {
    def apply(t: Try[_]) = {
      MatchResult(
        t.isSuccess,
        t.toString + " is not Success",
        t.toString + " is not Failure"
      )
    }
  }

  val success = new SuccessMatcher
  val failure = not (success)
}

/** Base spec for tests.
  */
class BaseSpec extends FlatSpec with Matchers with CustomMatchers


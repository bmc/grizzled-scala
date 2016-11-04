package grizzled

import java.io.{File, FileWriter}

import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.matchers.{BeMatcher, MatchResult}

import scala.sys.SystemProperties
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
class BaseSpec extends FlatSpec with Matchers with CustomMatchers {
  import grizzled.file.util.joinPath
  import grizzled.util.withResource
  import grizzled.util.CanReleaseResource.Implicits.CanReleaseAutoCloseable

  val lineSep = (new SystemProperties).getOrElse("line.separator", "\n")

  def createTextFile(dir: File, filename: String, contents: String): File = {
    val file = new File(joinPath(dir.getAbsolutePath, filename))
    withResource(new FileWriter(file)) { _.write(contents) }
    file
  }

  def createTextFile(dir: File, filename: String, contents: Array[String]): File = {
    createTextFile(dir, filename, contents.mkString(lineSep))
  }
}

package grizzled.util

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/** Enrichment classes, generally for things in `scala.util`.
  *
  */
object Implicits {
  /** Enriched `Try` class, containing some helper methods.
    *
    * @param t the underlying `Try`
    */
  implicit class RichTry[T](t: Try[T]) {

    /** Replacement for `scala.concurrent.Future.fromTry()`, which can't be
      * used here, because we still compile on Scala 2.10, and 2.10 doesn't
      * have that function. Converts a `Try` to a `Future`. A successful
      * `Try` (a `Success`) becomes a completed and successful `Future`.
      * A failed `Try` (a `Failure`) becomes a completed and failed `Future`.
      *
      * @return the corresponding `Future`.
      */
    def toFuture = t match {
      case Success(value) => Future.successful(value)
      case Failure(ex)    => Future.failed(ex)
    }
  }
}

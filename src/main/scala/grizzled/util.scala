package grizzled

import grizzled.ScalaCompat.scalautil.Using

import scala.annotation.implicitNotFound
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/** Miscellaneous utility functions and methods not otherwise categorized.
  */
package object util {
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
      def toFuture: Future[T] = t match {
        case Success(value) => Future.successful(value)
        case Failure(ex)    => Future.failed(ex)
      }
    }
  }

  /** `withResource()` needs an implicit evidence parameter of this type
    * to know how to release what's passed to it. Note that on Scala 2.13,
    * this type is just a type alias for `scala.util.Using.Releasable`.
    *
    * @tparam T the type (which must be contravariant to allow, for instance,
    *           a `T` of `Closeable` to apply to subclasses like `InputStream`).
    */
  @implicitNotFound("Can't find a CanReleaseSource[${T}] for withResource/tryWithResource")
  type CanReleaseResource[-T] = Using.Releasable[T]

  /** Companion object for `CanReleaseResource`.
    */
  object CanReleaseResource {

    /**
      * Note: Do _not_ import `grizzled.util.CanReleaseResource._` in Scala
      * 2.12.x. Doing so will cause compiler errors if you then attempt to use
      * `withResource` on a `scala.io.Source`, since `Source` is also a
      * `Closeable` in 2.12, and this object provides implicit evidence objects
      * for both. Use explicit imports for the evidence objects you need.
      */
    object Implicits {
      import java.io.Closeable
      import scala.io.Source

      /** Evidence for type `Closeable`.
        */
      implicit object CanReleaseCloseable
        extends CanReleaseResource[Closeable] {

        def release(c: Closeable) = c.close()
      }

      /** Evidence for type `Source`. Note that, in Scala 2.12.0,
        * `Source` is also `Closeable`.
        */
      implicit object CanReleaseSource extends CanReleaseResource[Source] {
        def release(s: Source) = s.close()
      }

      /** Evidence for type `AutoCloseable`, which allows the use of
        * `withResource` and `tryWithResource` with types such as
        * `java.sql.Connection`.
        */
      implicit object CanReleaseAutoCloseable
        extends CanReleaseResource[AutoCloseable] {

        def release(c: AutoCloseable) = c.close()
      }
    }
  }

  /** Ensure that a closeable object is closed. Note that this function
    * requires an implicit evidence parameter of type `CanClose` to determine
    * how to close the object. You can implement your own, though common
    * ones are provided automatically.
    *
    * Sample use:
    *
    * {{{
    * withResource(new java.io.FileInputStream("/path/to/file")) { in =>
    *   ...
    * }
    * }}}
    *
    * In Scala 2.13, you can use `scala.util.Using.resource` to accomplish the
    * the same thing:
    *
    * {{{
    * import scala.util.Using
    *
    * Using.resource(new java.io.FileInputStream("/path/to/file")) { in =>
    *   ...
    * }
    * }}}
    *
    * On Scala 2.13, `withResource` is implemented in terms of `Using.resource`.
    * On previous versions, it is implemented in terms of a `Using`
    * compatibility layer.
    *
    * '''Note''': If the block throws an exception, `withResource` propagates
    * the exception. If you want to capture the exception, instead, use
    * [[grizzled.util.tryWithResource]].
    *
    * @param resource  the object that holds a resource to be released
    * @param code      the code block to execute with the resource
    * @param mgr       the resource manager that can release the resource
    *
    * @tparam T    the type of the resource
    * @tparam R    the return type of the code block
    *
    * @return whatever the block returns
    */
  @inline
  final def withResource[T, R](resource: T)
                              (code: T => R)
                              (implicit mgr: CanReleaseResource[T]): R = {
    import ScalaCompat.scalautil.Using
    Using.resource(resource)(code)
  }

  /** A version of [[grizzled.util.withResource]] that captures any thrown
    * exception, instead of propagating it.
    *
    * Example:
    *
    * {{{
    * val t: Try[Unit] = tryWithResource(new java.io.FileInputStream("...")) { in =>
    * }
    * }}}
    *
    * In Scala 2.13, you can use `scala.util.Using.apply` to accomplish the
    * the same thing:
    *
    * {{{
    * import scala.util.Using
    *
    * val t: Try[Unit] = Using(new java.io.FileInputStream("/path/to/file")) { in =>
    *   ...
    * }
    * }}}
    *
    * On Scala 2.13, `tryWithResource` is implemented in terms of `Using.apply`.
    * On previous versions, it is implemented in terms of a `Using`
    * compatibility layer.
    *
    * @param open  the by-name parameter (code block) to open the resource.
    *              This parameter is a by-name parameter so that this
    *              function can capture any exceptions it throws.
    * @param code  the code block to execute with the resource
    * @param mgr   the resource manager that can release the resource
    *
    * @tparam T    the type of the resource
    * @tparam R    the return type of the code block
    *
    * @return A `Success` containing the result of the code block, or a
    *         `Failure` with any thrown exception.
    */
  @inline
  final def tryWithResource[T, R](open: => T)
                                (code: T => R)
                                (implicit mgr: CanReleaseResource[T]): Try[R] = {
    Try {
      withResource(open)(code)(mgr)
    }
  }
}


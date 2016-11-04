/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright © 2009-2016, Brian M. Clapper
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

   * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

   * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

   * Neither the names "clapper.org", "Grizzled Scala Library", nor the
    names of its contributors may be used to endorse or promote products
    derived from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  ---------------------------------------------------------------------------
 */

package grizzled

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
      def toFuture = t match {
        case Success(value) => Future.successful(value)
        case Failure(ex)    => Future.failed(ex)
      }
    }
  }

  /** `withResource()` needs an implicit evidence parameter of this type
    * to know how to release what's passed to it.
    *
    * @tparam T the type (which must be contravariant to allow, for instance,
    *           a `T` of `Closeable` to apply to subclasses like `InputStream`).
    */
  @implicitNotFound("Can't find a CanReleaseSource[${T}] for withCloseable()")
  trait CanReleaseResource[-T] {
    def release(a: T): Unit
  }

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

      /** Defines evidence for type `Closeable`.
        */
      implicit object CanReleaseCloseable
        extends CanReleaseResource[Closeable] {

        def release(c: Closeable) = c.close()
      }

      /** Defines evidence for type `Source`.
        */
      implicit object CanReleaseSource extends CanReleaseResource[Source] {
        def release(s: Source) = s.close()
      }

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
    * withResource(new java.io.FileInputStream("/path/to/file")) {
    *     in => ...
    * }
    * }}}
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
  def withResource[T, R](resource: T)
                         (code: T => R)
                         (implicit mgr: CanReleaseResource[T]): R = {
    try {
      code(resource)
    }

    finally {
      mgr.release(resource)
    }
  }

  /** A version of [[grizzled.util.withResource]] that captures any thrown
    * exception, instead of propagating it.
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
  def tryWithResource[T, R](open: => T)
                           (code: T => R)
                           (implicit mgr: CanReleaseResource[T]): Try[R] = {
    Try {
      withResource(open)(code)(mgr)
    }
  }
}


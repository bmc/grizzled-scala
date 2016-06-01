/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright © 2009-2016, Brian M. Clapper. All rights reserved.

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

import scala.reflect.ClassTag
import scala.util.{Success, Failure, Try}

/** Helpers for the Scala `Either` class.
  */
object either {

  /** Implicits. Import to get them.
    */
  object Implicits {

    /** Enriched `Either` class, providing `map` and `flatMap` methods
      * that map over and `Either` object if its value is `Right` (and
      * permit easier use of `Either` objects in `for` comprehensions).
      */
    implicit class RichEither[L,R](val e: Either[L, R]) extends AnyVal {

      /** Map an `Either` value only if it's a `Right`. If it's a
        * `Left`, just return the `Left` unmodified.
        *
        * This method is roughly analogous to the Scala `Option` class's
        * `map()` function.
        *
        * @param mapper Partial function taking a value of type `R`
        *               (i.e., what's stored in the `Right` side) and maps
        *               it to a value of type `R2`.
        * @tparam R2    the mapped `Right` type
        */
      def map[R2](mapper: R => R2): Either[L, R2] = {
        flatMap(r => Right(mapper(r)))
      }

      /** Similar to the `map()` method, this method invokes the
        * supplied partial function only if the supplied `Either` is a
        * `Right`. Unlike `map()`, however, `flatMap()` does
        * not automatically rewrap the result in a `Right`; instead, it
        * expects the supplied partial function to return an `Either`.
        *
        * This method is roughly analogous to the Scala `Option` class's
        * `flatMap()` function.
        *
        * @param mapper Partial function taking a value of type `R`
        *               (i.e., what's stored in the `Right` side) and maps
        *               it to an `Either[L, R2]`.
        * @tparam R2    the mapped `Right` type
        */
      def flatMap[R2](mapper: R => Either[L, R2]): Either[L, R2] = {
        e match {
          case Left(error)  => Left(error)
          case Right(value) => mapper(value)
        }
      }

      /** Convert an `Either` to a `Try`. If the `Either` is a `Right`, then
        * the value is extracted from the `Right` and wrapped in a `Success`.
        * If the `Either` is a `Left`, then the value is extracted from the
        * left, stored in an `Exception`, and wrapped in a `Failure`.
        *
        * To convert the exception to another exception, you can use
        * `.recoverWith`:
        *
        * {{{
        * either.toTry.recoverWith { case e => Failure(new MyException(e)) }
        * }}}
        *
        * @return the `Either`, converted to a `Try`
        */
      def toTry: Try[R] = {
        e match {
          case Left(error)  => Failure(new Exception(error.toString))
          case Right(value) => Success(value)
        }
      }
    }
  }
}


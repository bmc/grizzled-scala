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

import grizzled.either.Implicits._
import org.scalatest.Inside

import scala.util.{Failure, Success, Try}

/**
 * Tests the grizzled.either class.
 */
class EitherSpec extends BaseSpec with Inside {
  "map" should "work on a Right" in {
    val r = Right(true)
    r.map(_ => false) shouldBe Right(false)
  }

  it should "work on a Left" in {
    val r: Either[String, Boolean] = Left("no change")
    r.map(_ => false) shouldBe r
  }

  "flatMap" should "properly map a Right to a Right" in {
    val r = Right(true)
    r.flatMap(_ => Right(false)) shouldBe Right(false)
  }

  it should "properly map a Right to a Left" in {
    val r: Either[String, Boolean] = Right(true)
    r.flatMap(_ => Left("fail")) shouldBe Left("fail")
  }

  it should "properly map a Left to a Right" in {
    val r: Either[String, Boolean] = Left("fail")
    r.flatMap(_ => Right(true)) shouldBe Left("fail")
  }

  it should "properly map a Left to a Left by preserving the original" in {
    val r: Either[String, Boolean] = Left("fail")
    r.flatMap(_ => Left("fail 2")) shouldBe Left("fail")
  }

  "Either with a for comprehension" should "work with 2 Rights" in {
    val expected: Either[String, Int] = Right(30)
    val rv1: Either[String, Int] = Right(10)
    val rv2: Either[String, Int] = Right(20)

    (for { v1 <- rv1; v2 <- rv2 } yield v1 + v2) shouldBe expected
  }

  it should "work with a Right and a Left" in {
    val expected: Either[String, Int] = Left("foo")
    val rv1: Either[String, Int] = Right(10)
    val rv2: Either[String, Int] = Left("foo")

    (for { v1 <- rv1; v2 <- rv2 } yield v1 + v2) shouldBe expected
  }

  it should "work with 2 Lefts" in {
    val expected: Either[String, Int] = Left("bar")
    val rv1: Either[String, Int] = Left("bar")
    val rv2: Either[String, Int] = Left("foo")

    (for { v1 <- rv1; v2 <- rv2 } yield v1 + v2) shouldBe expected
  }

  "toTry" should "convert a Right to a Success" in {
    Right(10).toTry shouldBe Success(10)
  }

  it should "convert a Left(String) to a Failure" in {
    inside(Left("Oops").toTry) {
      case Failure(e: Exception) => e.getMessage shouldBe "Oops"
    }
  }

  it should "convert a Left(Int) to a Failure" in {
    inside(Left(-1).toTry) {
      case Failure(e: Exception) => e.getMessage shouldBe "-1"
    }
  }
}

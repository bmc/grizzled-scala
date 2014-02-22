/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2009-2014 Brian M. Clapper. All rights reserved.

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

import org.scalatest.FunSuite
import grizzled.either.Implicits._

/**
 * Tests the grizzled.either class.
 */
class EitherTest extends FunSuite {
  test("map - if right") {
    val r = Right(true)

    assertResult(Right(false), "Right(true) mapped to Right(false)") {
      r.map(_ => false)
    }
  }

  test("map - if left") {
    val r: Either[String, Boolean] = Left("no change")

    assertResult(r, "Left(string) not mapped Right(false)") {
      r.map(_ => false)
    }
  }

  test("flatMap - Right to Right") {
    val r = Right(true)

    assertResult(Right(false), "Right(true) flatMapped to Right(false)") {
      r.flatMap(_ => Right(false))
    }
  }

  test("flatMap - Right to Left") {
    val r: Either[String, Boolean] = Right(true)

    assertResult(Left("fail"), "Right(true) flatMapped to Left(err)") {
      r.flatMap(_ => Left("fail"))
    }
  }

  test("flatMap - Left to Right") {
    val r: Either[String, Boolean] = Left("fail")

    assertResult(Left("fail"), "Left(s) flatMapped to Right(true)") {
      r.flatMap(_ => Right(true))
    }
  }

  test("flatMap - Left to Left") {
    val r: Either[String, Boolean] = Left("fail")

    assertResult(Left("fail"), "Left(s) flatMapped to Left(s2)") {
      r.flatMap(_ => Left("fail 2"))
    }
  }

  test("Either - for comprehension with two Rights") {
    val expected: Either[String, Int] = Right(30)

    assertResult(expected, "for over two Right objects") {
      val rv1: Either[String, Int] = Right(10)
      val rv2: Either[String, Int] = Right(20)

      for { v1 <- rv1; v2 <- rv2 } yield v1 + v2
    }
  }

  test("Either - for comprehension with a Right and a Left") {
    val expected: Either[String, Int] = Left("foo")

    assertResult(expected, "for over a Left and a Right") {
      val rv1: Either[String, Int] = Right(10)
      val rv2: Either[String, Int] = Left("foo")

      for { v1 <- rv1; v2 <- rv2 } yield v1 + v2
    }
  }

  test("Either - for comprehension with two Lefts") {
    val expected: Either[String, Int] = Left("bar")

    assertResult(expected, "for over a Left and a Right") {
      val rv1: Either[String, Int] = Left("bar")
      val rv2: Either[String, Int] = Left("foo")

      for { v1 <- rv1; v2 <- rv2 } yield v1 + v2
    }
  }
}

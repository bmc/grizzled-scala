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

package grizzled.io

import org.scalatest.{FlatSpec, Matchers}
import scala.io.Source

/**
  * Tests the grizzled.file.GrizzledSource functions.
  */
class GrizzledSourceSpec extends FlatSpec with Matchers {
  "First nonblank line" should "skip blank lines" in {
    import grizzled.io.Implicits._

    val data = List(("\n\n\n\nfoo\n\n\n", Some("foo")),
                    ("\n\n\n\n\n\n\n\n", None),
                    ("", None),
                    ("foobar", Some("foobar")) )

    for((input, expected) <- data) {
      val source = Source.fromString(input)
      source.firstNonblankLine shouldBe expected
    }
  }

  "linesBetween" should "properly throw away lines outside the range" in {
    import grizzled.io.Implicits._

    val data = List(
      ("{{\na\n}}\n", "{{", "}}", List("a")),
      ("*\nfoo\nbar\nbaz\n*\n", "*", "*", List("foo", "bar", "baz")),
      ("{{\n}}\n", "{{", "}}", Nil),
      ("{{\n\n}}\n", "{{", "}}", List("")),
      ("{{\n", "{{", "}}", Nil),
      ("\n\n\n", "{{", "}}", Nil),
      ("\n\n\n}}", "{{", "}}", Nil)
    )

    for((input, start, end, expected) <- data) {
      val source = Source.fromString(input)
      source.linesBetween(start, end).toList shouldBe expected
    }
  }
}

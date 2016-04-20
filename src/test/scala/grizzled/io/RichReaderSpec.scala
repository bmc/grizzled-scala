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

import java.io.StringReader

import org.scalatest.{FlatSpec, Matchers}

class RichReaderSpec extends FlatSpec with Matchers {

  "readSome" should "stop reading when it hits the max" in {
    import grizzled.io.Implicits.RichReader

    val data = List(
      ("12345678901234567890", 10, "1234567890"),
      ("12345678901234567890", 30, "12345678901234567890"),
      ("12345678901234567890", 20, "12345678901234567890")
    )

    for((input, max, expected) <- data) {
      val r = new StringReader(input)
      r.readSome(max).mkString shouldBe expected
    }
  }

  it should "handle a max that's larger than the input" in {
    import grizzled.io.Implicits.RichReader

    val s = "1234"
    val r = new StringReader(s)
    r.readSome(1000).mkString shouldBe s
  }

  it should "handle an empty input" in {
    import grizzled.io.Implicits.RichReader

    val r = new StringReader("")
    r.readSome(1000).mkString shouldBe ""
  }

  "copyTo" should "copy chars from a reader to a writer" in {
    import java.io.{StringReader, StringWriter}
    import grizzled.io.Implicits.RichReader

    val data = List("12345678901234567890",
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    "a",
                    "")

    for(s <- data) {
      val r = new StringReader(s)
      val w = new StringWriter
      r.copyTo(w)
      w.toString shouldBe s
    }
  }
}

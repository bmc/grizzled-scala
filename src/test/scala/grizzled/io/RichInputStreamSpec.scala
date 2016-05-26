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

import java.io.ByteArrayInputStream

import grizzled.BaseSpec

class RichInputStreamSpec extends BaseSpec {
  import grizzled.io.Implicits.RichInputStream

  "readSome" should "stop reading when it hits the max" in {
    val input = Array[Byte]( 1,  2,  3,  4,  5,  6,  7,  8,  9, 10,
                            11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
    val data = List(
      (10, input.slice(0, 10)),
      (20, input),
      (30, input)
    )

    for((max, expected) <- data) {
      val is = new ByteArrayInputStream(input)
      is.readSome(max) shouldBe expected
    }
  }

  it should "handle a max that's larger than the input" in {
    val input = Array[Byte](1, 2, 3, 4)
    val is = new ByteArrayInputStream(input)
    is.readSome(1000) shouldBe input
  }

  it should "handle an empty input" in {
    val input = Array.empty[Byte]
    val is = new ByteArrayInputStream(input)
    is.readSome(1000) shouldBe input
  }

  "copyTo" should "copy bytes from an InputStream to an OutputStream" in {
    import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

    val input = List[Byte]( 1,  2,  3,  4,  5,  6,  7,  8,  9, 10,
                           11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
    val data = List(input.slice(0, 10),
                    input,
                    input.slice(0, 1))

    for(bytes <- data) {
      val is = new ByteArrayInputStream(bytes.toArray)
      val os = new ByteArrayOutputStream
      is.copyTo(os)
      os.toByteArray.toList shouldBe bytes
    }
  }

  it should "work fine with with big input" in {
    // will fail with java.lang.StackOverflowError if copyTo was
    // not tail-call optimized

    import java.io.{InputStream, OutputStream}
    import java.util.Random

    val rnd = new Random()
    val inp = new InputStream {
      var countDown = 5000

      override def read(): Int = {
        if (countDown > 0) {
          countDown -= 1
          rnd.nextInt(256)
        }
        else
          -1
      }
    }

    val nul = new OutputStream {
      override def write(b: Int) = {}
    }

    try {
      inp.copyTo(nul)
    }
    catch {
      case e: StackOverflowError =>
        fail("StackOverflowError - copyTo not tail-call optimized?")
    }
  }
}

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

import org.scalatest.FunSuite
import grizzled.io.RichReader._
import grizzled.io.RichInputStream._

/**
 * Tests the grizzled.io functions.
 */
class IOTest extends FunSuite {
  test("RichReader.readSome") {
    import java.io.StringReader

    val data = List(
      ("12345678901234567890", 10, "1234567890"),
      ("12345678901234567890", 30, "12345678901234567890"),
      ("12345678901234567890", 20, "12345678901234567890")
    )

    for((input, max, expected) <- data) {
      assertResult(expected, "RichReader.readSome(" + max + ") on: " + input) {
        val r = new StringReader(input)
        r.readSome(max) mkString ""
      }
    }
  }

  test("RichInputStream.readSome") {
    import java.io.ByteArrayInputStream

    val input = List[Byte]( 1,  2,  3,  4,  5,  6,  7,  8,  9, 10,
                           11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
    val inputArray = input.toArray
    val data = List(
      (10, input.slice(0, 10)),
      (20, input),
      (30, input)
    )

    for((max, expected) <- data) {
      assertResult(expected, 
             "RichInputStream.readSome(%d) on: %s" format (max, inputArray)) {
        val is = new ByteArrayInputStream(inputArray)
        is.readSome(max)
      }
    }
  }

  test("RichReader.copyTo") {
    import java.io.{StringReader, StringWriter}

    val data = List("12345678901234567890",
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    "a",
                    "")

    for(s <- data) {
      assertResult(s, "RichReader.copyTo() on: " + s) {
        val r = new StringReader(s)
        val w = new StringWriter
        r.copyTo(w)
        w.toString
      }
    }
  }

  test("RichInputStream.copyTo") {
    import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

    val input = List[Byte]( 1,  2,  3,  4,  5,  6,  7,  8,  9, 10,
                           11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
    val data = List(input.slice(0, 10),
                    input,
                    input.slice(0, 1))

    for(bytes <- data) {
      assertResult(bytes, "RichInputStream.copyTo() on: " + bytes) {
        val is = new ByteArrayInputStream(bytes.toArray)
        val os = new ByteArrayOutputStream
        is.copyTo(os)
        os.toByteArray.toList
      }
    }
  }

  test("RichInputStream.copyTo with big input") {
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

  test("withCloseable") {
    import java.io.{FileOutputStream, File}
    import java.nio.channels.Channels
    import grizzled.io.util._

    val temp = File.createTempFile("test", ".dat")
    temp.deleteOnExit

    val fs = new FileOutputStream(temp)
    assertResult(false, "withCloseable") {
      val chan = Channels.newChannel(fs)
      withCloseable(chan) { chan => assert(chan.isOpen) }
      chan.isOpen
    }
  }
}

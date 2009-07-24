/*---------------------------------------------------------------------------*\
  This software is released under a BSD-style license:

  Copyright (c) 2009 Brian M. Clapper. All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

  1.  Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.

  2.  The end-user documentation included with the redistribution, if any,
      must include the following acknowlegement:

        "This product includes software developed by Brian M. Clapper
        (bmc@clapper.org, http://www.clapper.org/bmc/). That software is
        copyright (c) 2009 Brian M. Clapper."

      Alternately, this acknowlegement may appear in the software itself,
      if wherever such third-party acknowlegements normally appear.

  3.  Neither the names "clapper.org", "The Grizzled Scala Library",
      nor any of the names of the project contributors may be used to
      endorse or promote products derived from this software without prior
      written permission. For written permission, please contact
      bmc@clapper.org.

  4.  Products derived from this software may not be called "clapper.org
      Java Utility Library", nor may "clapper.org" appear in their names
      without prior written permission of Brian M. Clapper.

  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
  NO EVENT SHALL BRIAN M. CLAPPER BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
\*---------------------------------------------------------------------------*/

import org.scalatest.FunSuite
import grizzled.io.implicits._

/**
 * Tests the grizzled.io functions.
 */
class IOTest extends GrizzledFunSuite
{
    test("RichReader.readSome")
    {
        import java.io.StringReader

        val data = List(
            ("12345678901234567890", 10, "1234567890"),
            ("12345678901234567890", 30, "12345678901234567890"),
            ("12345678901234567890", 20, "12345678901234567890")
        )

        for((input, max, expected) <- data)
            expect(expected, "RichReader.readSome(" + max + ") on: " + input)
            {
                val r = new StringReader(input)
                r.readSome(max) mkString ""
            }
    }

    test("RichInputStream.readSome")
    {
        import java.io.ByteArrayInputStream

        val input = List[Byte]( 1,  2,  3,  4,  5,  6,  7,  8,  9, 10,
                               11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
        val inputArray = input.toArray
        val data = List(
            (10, input.slice(0, 10)),
            (20, input),
            (30, input)
        )

        for((max, expected) <- data)
            expect(expected, 
                   "RichInputStream.readSome(" + max + ") on: " + inputArray)
            {
                val is = new ByteArrayInputStream(inputArray)
                is.readSome(max)
            }
    }

    test("RichReader.copyTo")
    {
        import java.io.{StringReader, StringWriter}

        val data = List("12345678901234567890",
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        "a",
                        "")

        for(s <- data)
            expect(s, "RichReader.copyTo() on: " + s)
            {
                val r = new StringReader(s)
                val w = new StringWriter
                r.copyTo(w)
                w.toString
            }
    }

    test("RichInputStream.copyTo")
    {
        import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

        val input = List[Byte]( 1,  2,  3,  4,  5,  6,  7,  8,  9, 10,
                               11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
        val data = List(input.slice(0, 10),
                        input,
                        input.slice(0, 1)
        )

        for(bytes <- data)
            expect(bytes, "RichInputStream.copyTo() on: " + bytes)
            {
                val is = new ByteArrayInputStream(bytes.toArray)
                val os = new ByteArrayOutputStream
                is.copyTo(os)
                os.toByteArray.toList
            }
    }
}

/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2009-2010 Brian M. Clapper. All rights reserved.

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
import grizzled.binary._

/**
 * Tests the grizzled.binary functions.
 */
class BinaryTest extends GrizzledFunSuite
{
    test("bitCount")
    {
        val intData = Map[Int, Int](
            0                -> 0,
            1                -> 1,
            2                -> 1,
            3                -> 2,
            0x44444444       -> 8,
            0xeeeeeeee       -> 24,
            0xffffffff       -> 32,
            0x7fffffff       -> 31
        )

        val longData = Map[Long, Int](
            0l                   -> 0,
            1l                   -> 1,
            2l                   -> 1,
            3l                   -> 2,
            0x444444444l         -> 9,
            0xeeeeeeeeel         -> 27,
            0xffffffffl          -> 32,
            0x7fffffffl          -> 31,
            0xffffffffffffl      -> 48
        )

        for((n, expected) <- intData)
        {
            expect(expected, "\"" + n.toString + "\" -> " + expected.toString) 
            {
                bitCount(n)
            }
        }

        for((n, expected) <- longData)
        {
            expect(expected, "\"" + n.toString + "\" -> " + expected.toString) 
            {
                bitCount(n)
            }
        }
    }
}

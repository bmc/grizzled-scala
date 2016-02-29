/*
  ---------------------------------------------------------------------------
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

package grizzled.string

import org.scalatest.{FlatSpec, Matchers}
import grizzled.string.GrizzledChar._

/**
 * Tests the GrizzledChar class.
 */
class GrizzledCharSpec extends FlatSpec with Matchers {
  "isHexDigit" should "detect valid hex digits and reject invalid ones" in {
    val data = Map('0' -> true,
                   '1' -> true,
                   '2' -> true,
                   '3' -> true,
                   '4' -> true,
                   '5' -> true,
                   '6' -> true,
                   '7' -> true,
                   '8' -> true,
                   '9' -> true,
                   'a' -> true,
                   'A' -> true,
                   'b' -> true,
                   'B' -> true,
                   'c' -> true,
                   'C' -> true,
                   'd' -> true,
                   'D' -> true,
                   'e' -> true,
                   'E' -> true,
                   'f' -> true,
                   'F' -> true,
                   'g' -> false,
                   'G' -> false,
                   '!' -> false,
                   ':' -> false,
                   '+' -> false,
                   '-' -> false,
                   '.' -> false)

    for((c, expected) <- data) {
      c.isHexDigit shouldBe expected
    }
  }
}

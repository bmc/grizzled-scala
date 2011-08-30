/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2009, Brian M. Clapper
  All rights reserved.

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

/** Useful binary-related utility functions.
  */
object binary {
  /** Count the number of bits in a numeric (integer or long) value. This
    * method is adapted from the Hamming Weight algorithm. It works for
    * up to 64 bits.
    *
    * @param num  the numeric value
    *
    * @return the number of 1 bits in a binary representation of `num`
    */
  def bitCount(num: Int): Int = {
    val numLong: Long = num.toLong
    bitCount(numLong & 0xffffffffl)
  }

  /** Count the number of bits in a numeric (integer or long) value. This
    * method is adapted from the Hamming Weight algorithm. It works for
    * up to 64 bits.
    *
    * @param num  the numeric value
    *
    * @return the number of 1 bits in a binary representation of `num`
    */
  def bitCount(num: Long): Int = {
    // Put count of each 2 bits into those 2 bits.
    val res1: Long = num - ((num >> 1) & 0x5555555555555555l)

    // Put count of each 4 bits into those 4 bits.
    val allThrees = 0x3333333333333333l
    val res2 = (res1 & allThrees) + ((res1 >> 2) & allThrees)

    // Put count of each 8 bits into those 8 bits.
    val res3 = (res2 + (res2 >> 4)) & 0x0f0f0f0f0f0f0f0fl

    // Left-most bits.
    ((res3 * 0x0101010101010101l) >> 56) toInt
  }
}

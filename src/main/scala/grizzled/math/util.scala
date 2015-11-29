/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright © 2009-2016, Brian M. Clapper
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

package grizzled.math

import scala.math

/** Useful math-related utility functions.
  */
object util {

  /** A `max` method that works with any number of integers.
    *
    * @param args  integers from which to find the max
    *
    * @return the maximum
    */
  def max(args: Int*) = (0 /: args)((a, b) => math.max(a, b))

  /** A `max` method that works with any number of longs.
    *
    * @param args  longs from which to find the max
    *
    * @return the maximum
    */
  def max(args: Long*) = (0L /: args)((a, b) => math.max(a, b))

  /** A `max` method that works with any number of floats.
    *
    * @param args  floats from which to find the max
    *
    * @return the maximum
    */
  def max(args: Float*) = (0.0f /: args)((a, b) => math.max(a, b))

  /** A `max` method that works with any number of doubles.
    *
    * @param args  doubles from which to find the max
    *
    * @return the maximum
    */
  def max(args: Double*) = (0.0 /: args)((a, b) => math.max(a, b))

  /** A `min` method that works with any number of integers.
    *
    * @param args  integers from which to find the min
    *
    * @return the minimum
    */
  def min(args: Int*) = (0 /: args)((a, b) => math.min(a, b))

  /** A `min` method that works with any number of longs.
    *
    * @param args  longs from which to find the min
    *
    * @return the minimum
    */
  def min(args: Long*) = (0L /: args)((a, b) => math.min(a, b))

  /** A `min` method that works with any number of floats.
    *
    * @param args  floats from which to find the min
    *
    * @return the minimum
    */
  def min(args: Float*) = (0.0f /: args)((a, b) => math.min(a, b))

  /** A `min` method that works with any number of doubles.
    *
    * @param args  doubles from which to find the min
    *
    * @return the minimum
    */
  def min(args: Double*) = (0.0 /: args)((a, b) => math.min(a, b))
}

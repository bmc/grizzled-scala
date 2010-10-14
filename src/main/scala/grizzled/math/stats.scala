/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2009-2010, Brian M. Clapper
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

/**
 * Miscellaneous statistics-related functions. You must import
 * `scala.math.Numeric` for these functions to work.
 */
object stats
{
    // See http://scala-programming-language.1934581.n4.nabble.com/How-to-use-scala-math-Numeric-td2013703.html

    /**
     * Calculates the geometric mean of the values of the passed-in
     * numbers, namely, the n-th root of (x1 * x2 * ... * xn).
     *
     * @param n the numbers on which to operate
     *
     * @return the geometric mean
     */
    def geometricMean[T](n: T*)(implicit x: Numeric[T]): Double =
    {
        val nList = n.toList
        val len = nList.length
        require (len > 0)

        len match
        {
            case 1 =>
                x.toDouble(nList(0))

            case _ =>
                val recip = 1.0 / len.toDouble
                (1.0 /: nList) ((m, n) => m * math.pow(x.toDouble(n), recip))
        }
    }

    /**
     * Calculates the harmonic mean of the values of the passed-in
     * numbers, namely: n / (1/x1 + 1/x2 + ... + 1/xn).
     *
     * @param n the numbers on which to operate
     *
     * @return the harmonic mean
     */
    def harmonicMean[T](n: T*)(implicit x: Numeric[T]): Double =
    {
        val nList = n.toList
        val len = nList.length
        require (len > 0)

        len match
        {
            case 1 =>
                x.toDouble(nList(0))

            case _ =>
                len / ((0.0 /: nList) ((m, n) => m + (1.0 / x.toDouble(n))))
        }
    }

    /**
     * Calculates the arithmetic mean of the values of the passed-in
     * numbers.
     *
     * @param n the numbers on which to operate
     *
     * @return the arithmetic mean
     */
    def mean[T](n: T*)(implicit x: Numeric[T]): Double =
    {
        val nList = n.toList
        val len = nList.length
        require (len > 0)

        len match
        {
            case 1 =>
                x.toDouble(nList(0))

            case _ =>
                ((0.0 /: nList) ((m, n) => m + x.toDouble(n))) / len
        }
    }

    /**
     * Calculates the median of the values of the passed-in numbers.
     *
     * @param n the numbers on which to operate
     *
     * @return the median
     */
    def median[T](n: T*)(implicit x: Numeric[T]): Double =
    {
        val nList = n.toList
        val len = nList.length
        require (len > 0)

        if (len == 1)
            x.toDouble(nList(0))

        else
        {
            val sorted = nList sortWith (x.compare(_, _) < 0)
            val midpoint = sorted.length / 2
            len % 2 match
            {
                case 0 => // even
                    mean(x.toDouble(sorted(midpoint)),
                         x.toDouble(sorted(midpoint - 1)))


                case 1 => // odd
                    x.toDouble(sorted(midpoint))
            }
        }
    }

    /**
     * Calculates the mode (most common value(s)) of the values of the
     * passed-in numbers. If there are multiple common values, they're all
     * returned.
     *
     * @param n the numbers on which to operate
     *
     * @return list of modal values
     */
    def mode[T](n: T*)(implicit x: Numeric[T]): List[T] =
    {
        val nList = n.toList
        val len = nList.length
        require (len > 0)

        if (len == 1)
            nList take 1

        else
        {
            import scala.collection.mutable.{Map => MutableMap}

            val m = MutableMap.empty[T, Int]

            // Count the occurrences of each value.
            n.toList.foreach(t => m += t -> (m.getOrElse(t, 0) + 1))

            // Find the maximum count.
            val max = (0 /: m.values) (scala.math.max(_, _))

            // Extract the keys with values that match
            m filter ( tup => tup._2 == max ) map ( tup => tup._1 ) toList
        }
    }

    /**
     * Calculate the variance of the specified values, using N-1 for the
     * denominator. Useful for estimating population variance.
     *
     * @param n the numbers on which to operate
     *
     * @return the variance
     */
    def variance[T](n: T*)(implicit x: Numeric[T]): Double =
    {
        def sumOfSquares(dList: List[Double]): Double =
            (0.0 /: dList) ((sum, d) => sum + (d * d))

        val nList = n.toList
        val len = nList.length
        require (len > 1)

        val mn = mean(nList: _*)
        val deviations = nList map (x.toDouble(_) - mn)
        sumOfSquares(deviations) / ((len - 1).toDouble)
    }

    /**
     * Calculate the standard deviation of the specified values, using N-1
     * for the denominator. Useful for estimating population standard
     * deviation.
     *
     * @param n the numbers on which to operate
     *
     * @return the standard deviation
     */
    def stddev[T](n: T*)(implicit x: Numeric[T]): Double =
        java.lang.Math.sqrt(variance(n.toList: _*))
}

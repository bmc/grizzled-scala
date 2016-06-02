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

package grizzled.collection

import scala.collection.immutable.LinearSeq
import scala.language.implicitConversions
import java.util.{Collection => JCollection, Iterator => JIterator}

import scala.sys.SystemProperties

/** Enrichment classes for collections.
  */
object Implicits {
  /** Useful for converting a collection into an object suitable for use with
    * Scala's `for` loop.
    */
  implicit class CollectionIterator[T](val iterator: JIterator[T])
    extends Iterator[T] {

    def this(c: JCollection[T]) = this(c.iterator)

    def hasNext: Boolean = iterator.hasNext

    def next: T = iterator.next
  }

  /** An enrichment class that decorates a `LinearSeq`.
    *
    * @param underlying the underlying `LinearSeq`
    * @tparam T the type
    */
  implicit class GrizzledLinearSeq[+T](val underlying: LinearSeq[T])
    extends AnyVal {

    def realSeq = underlying

    /** Create a string containing the contents of this sequence, arranged
      * in columns.
      *
      * @param width total (maximum) columns
      *
      * @return a possibly multiline string containing the columnar output.
      *         The string may have embedded newlines, but it will not end
      *         with a newline.
      */
    def columnarize(width: Int): String = {
      import scala.collection.mutable.ArrayBuffer
      import grizzled.math.util.{max => maxnum}

      val lineSep = (new SystemProperties).getOrElse("line.separator", "\n")

      val buf = new ArrayBuffer[Char]

      // Lay them out in columns. Simple-minded for now.
      val strings: List[String] = underlying.map(_.toString).toList
      val colSize = maxnum(strings.map(_.length): _*) + 2
      val colsPerLine = width / colSize

      for ((s, i) <- strings.zipWithIndex) {
        val count = i + 1
        val padding = " " * (colSize - s.length)
        buf ++= (s + padding)
        if ((count % colsPerLine) == 0)
          buf ++= lineSep
      }

      buf mkString ""
    }

    override def toString = underlying.toString
  }

  /** An `Iterable` enrichment class.
    *
    * @param underlying the underlying iterable
    * @tparam T the iterable type
    */
  implicit class GrizzledIterable[+T](protected val underlying: Iterable[T])
    extends Iterable[T] {
    def self = this

    def realIterable = underlying

    def iterator = underlying.iterator


    /** Create a string containing the contents of this iterable, arranged
      * in columns.
      *
      * @param width    total (maximum) columns
      *
      * @return a possibly multiline string containing the columnar output.
      *         The string may have embedded newlines, but it will not end
      *         with a newline.
      */
    def columnarize(width: Int): String = {
      new GrizzledLinearSeq(underlying.toList).columnarize(width)
    }
  }
}


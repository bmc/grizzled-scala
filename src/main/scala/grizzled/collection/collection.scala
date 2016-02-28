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

import scala.collection.generic._
import scala.collection.immutable.LinearSeq
import scala.language.implicitConversions
import java.util.{Collection, Iterator => JIterator}
import scala.collection.IterableProxy

/** Useful for converting a collection into an object suitable for use with
  * Scala's `for` loop.
  */
class CollectionIterator[T](val iterator: JIterator[T]) extends Iterator[T] {

  /** Alternate constructor that takes a collection.
    *
    * @param collection  the collection
    */
  def this(collection: Collection[T]) = this(collection.iterator)

  def hasNext: Boolean = iterator.hasNext
  def next: T = iterator.next
}

object CollectionIterator {
  implicit def javaCollectionToScalaIterator[T](c: Collection[T]) =
    new CollectionIterator[T](c)
}

/** An iterator that iterates, serially, over the contents of multiple other
  * iterators.
  *
  * @param iterators  the iterators to wrap
  */
class MultiIterator[+T](iterators: Iterator[T]*) extends Iterator[T] {
  private[this] val iteratorList: List[Iterator[T]] = iterators.toList
  private[this] var current = iteratorList.head
  private[this] var nextIterators = iteratorList.tail

  /** Determines whether the iterator is empty. A `MultiIterator`
    * is empty when all contained iterators have been exhausted.
    *
    * @return `true` if there's more to read, `false` if not
    */
  def hasNext: Boolean = {
    if( current.hasNext )
      true

    else if( nextIterators == Nil )
      false

    else {
      current = nextIterators.head
      nextIterators = nextIterators.tail
      hasNext
    }
  }

  /** Get the next element.
    *
    * @return the next element
    */
  def next: T = {
    if (! hasNext)
      throw new java.util.NoSuchElementException

    current.next
  }
}

class GrizzledIterable[+T](protected val underlying: Iterable[T])
extends Iterable[T] {
  def self = this

  def realIterable = underlying

  def iterator = underlying.iterator

  def columnarize(width: Int): String = {
    new GrizzledLinearSeq(underlying.toList).columnarize(width)
  }
}

object GrizzledIterable {
  /** Convert a Scala Iterable object to a GrizzledIterable.
    */
  implicit def IterableToGrizzledIterable[T](it: Iterable[T]) =
    new GrizzledIterable[T](it)

  /** Convert GrizzledIterator a object to a Scala Iterable.
    */
  implicit def grizzledIterableToIterable[T](it: GrizzledIterable[T]) =
    it.realIterable
}

class GrizzledLinearSeq[+T](protected val underlying: LinearSeq[T]) {
  def realSeq = underlying

  /** Create a string containing the contents of this sequence, arranged
    * in columns.
    *
    * @param width    total (maximum) columns
    *
    * @return a possibly multiline string containing the columnar output.
    *         The string may have embedded newlines, but it will not end
    *         with a newline.
    */
  def columnarize(width: Int): String = {
    import scala.collection.mutable.ArrayBuffer
    import grizzled.math.util.{max => maxnum}

    val buf = new ArrayBuffer[Char]

    // Lay them out in columns. Simple-minded for now.
    println(underlying)
    val strings = underlying.map(_.toString).toList
    println(s">>> strings=$strings")
    val colSize = maxnum(strings.map(_.length): _*) + 2
    val colsPerLine = width / colSize
    println(s"*** width=$width, colSize=$colSize, colsPerLine=$colsPerLine")
    for ((s, i) <- strings.zipWithIndex) {
      val count = i + 1
      val padding = " " * (colSize - s.length)
      buf ++= (s + padding)
      if ((count % colsPerLine) == 0)
        buf += '\n'
    }

    buf mkString ""
  }

  override def toString = underlying.toString
}

/** Implicit conversions specific to GrizzledLinearSeq.
  */
object GrizzledLinearSeq {
  /** Convert a Scala Seq object to a GrizzledLinearSeq.
    */
  implicit def scalaSeqToGrizzledLinearSeq[T](seq: LinearSeq[T]) =
    new GrizzledLinearSeq[T](seq)

  /** Convert a Scala List object to a GrizzledLinearSeq.
    */
  implicit def listToGrizzledLinearSeq[T](list: List[T]) =
    new GrizzledLinearSeq[T](list)

  /** Convert GrizzledLinearSeq a object to a Scala Seq.
    */
  implicit def grizzledLinearSeqToScalaSeq[T](seq: GrizzledLinearSeq[T]) =
    seq.realSeq
}

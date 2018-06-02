package grizzled.collection

import scala.collection.immutable.LinearSeq
import scala.language.implicitConversions
import java.util.{Collection => JCollection, Iterator => JIterator}

import scala.annotation.tailrec
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.{Builder => MutableBuilder}
import scala.sys.SystemProperties
import scala.language.higherKinds

/** Enrichment classes for collections.
  */
object Implicits {


  import scala.collection.Iterable

  /** Can be used to add a `mapWhile()` function to a collection type.
    *
    * @tparam T  type of value contained in the collection
    * @tparam I  the collection, which must be a subclass of `Iterable[T]`
    */
  trait MapWhile[+T, I <: Iterable[T]] {
    val container: I

    /** Perform a map on an `Iterable` subclass until the predicate function
      * returns `false`. Combines `map()` and `takeWhile()` in a more efficient
      * manner. The predicate is called ''after'' the mapping operation is
      * performed.
      *
      * @param mapper    the mapper function
      * @param predicate the predicate. The mapping operation will continue
      *                  until this function returns `false` or the iterable
      *                  is exhausted
      * @param cbf       the `CanBuildFrom` factory, which allows this function
      *                  to make an instance of the subclass without knowing
      *                  what it is
      * @tparam U        the type of the returned subclass
      * @tparam J        the type of the returned `Iterable` subclass
      * @return          the mapped (and possibly filtered) result
      */
    def mapWhile[U, J](mapper: T => U, predicate: U => Boolean)
                      (implicit cbf: CanBuildFrom[List[T], U, J]): J = {
      @tailrec
      def loop(xs: List[T], acc: MutableBuilder[U, J]): J = {
        xs match {
          case Nil =>
            acc.result
          case head :: tail =>
            val b = mapper(head)
            if (!predicate(b))
              acc.result
            else {
              acc += b
              loop(tail, acc)
          }
        }
      }

      loop(container.toList, cbf())
    }
  }

  /** Adds a `mapWhile()` function to an `Iterable`.
    *
    * @param  i   the iterable
    * @tparam T   the type of the iterable
    *
    * @return a `MapWhile` object with a `mapWhile()` function that returns a
    *         new `Iterator[T]`
    */
  implicit def iterableMapWhile[T](i: Iterable[T]): MapWhile[T, Iterable[T]] = {
    new MapWhile[T, Iterable[T]] { val container = i }
  }

  /** Adds a `mapWhile()` function to a `Seq`.
    *
    * @param  seq the sequence
    * @tparam T   the type of the sequence
    *
    * @return a `MapWhile` object with a `mapWhile()` function that returns a
    *         new `Seq[T]`
    */
  implicit def seqMapWhile[T](seq: Seq[T]): MapWhile[T, Seq[T]] = {
    new MapWhile[T, Seq[T]] { val container = seq }
  }

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
    * @param container the underlying `LinearSeq`
    * @tparam T the type
    */
  implicit class GrizzledLinearSeq[+T](val container: LinearSeq[T])
    extends {

    def realSeq: Seq[T] = container

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
      val strings: Seq[String] = container.map(_.toString)
      val colSize = strings match {
        case s if s.isEmpty =>
          0
        case Seq(s) =>
          s.length
        case Seq(head, tail @ _*) =>
          maxnum(head.length, tail.map(_.length): _*) + 2
      }

      val colsPerLine = width / colSize

      strings
        .zipWithIndex
        .map { case (s: String, i: Int) =>
          val count = i + 1
          val padding = " " * (colSize - s.length)
          val sep = if ((count % colsPerLine) == 0) lineSep else ""
          s + padding + sep
        }
        .mkString("")
    }

    override def toString = container.toString
  }

  /** An `Iterable` enrichment class.
    *
    * @param container the underlying iterable
    * @tparam T the iterable type
    */
  implicit class GrizzledIterable[+T](val container: Iterable[T])
    extends Iterable[T] {
    def self: GrizzledIterable[T] = this

    def realIterable: Iterable[T] = container

    def iterator: Iterator[T] = container.iterator


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
      new GrizzledLinearSeq(container.toList).columnarize(width)
    }
  }
}

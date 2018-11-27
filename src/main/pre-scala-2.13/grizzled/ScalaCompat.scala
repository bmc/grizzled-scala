package grizzled

/** Compatibility definitions for Scala 2.13+ vs. Scala 2.12 and lesser.
  * This object is conceptually similar to `scala.collection.compat`. For
  * Scala 2.12 and earlier, it provides a type alias and compatibility functions
  * for `LazyList`. For Scala 2.13 and greater, it's empty. Thus, all code
  * can use `LazyList` throughout.
  */
package object ScalaCompat {
  type LazyList[+T] = Stream[T]

  object LazyList {
    def empty[T]: LazyList[T] = Stream.empty[T]

    object #:: {
      @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
      def unapply[T](s: LazyList[T]): Option[(T, LazyList[T])] =
        if (s.nonEmpty) Some((s.head, s.tail)) else None
    }

  }
}

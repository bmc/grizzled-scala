package grizzled

/** Compatibility definitions for Scala 2.13+ vs. Scala 2.12 and lesser.
  * This object is conceptually similar to `scala.collection.compat`. For
  * Scala 2.12 and earlier, it provides a type alias and compatibility functions
  * for `LazyList`. For Scala 2.13 and greater, it's empty. Thus, all code
  * can use `LazyList` throughout.
  */
package object ScalaCompat {
}

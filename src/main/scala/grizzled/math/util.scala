package grizzled.math

/** Useful math-related utility functions.
  */
object util {

  /** A `max` function that will take any number of arguments for which an
    * `Ordering` is defined, returning the "largest" one, as defined by the
    * `Ordering`.
    *
    * @param arg   the first item
    * @param args  the remaining items for which to find the maximum
    * @tparam T    the argument type
    *
    * @return the maximum value
    */
  // reduce() is okay here, since the argument list cannot be empty.
  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  def max[T: Ordering](arg: T, args: T*): T = {
    (arg +: args).reduce { (a: T, b: T) =>
      val ev = implicitly[Ordering[T]]
      ev.compare(a, b) match {
        case i if i < 0 => b
        case i if i > 0 => a
        case _          => a
      }
    }
  }

  /** A `max` function that will take any number of arguments for which an
    * `Ordering` is defined, returning the "largest" one, as defined by the
    * `Ordering`.
    *
    * @param arg   the first item
    * @param args  the remaining items for which to find the maximum
    * @tparam T    the argument type
    *
    * @return the maximum value
    */
  // reduce() is okay here, since the argument list cannot be empty.
  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  def min[T: Ordering](arg: T, args: T*): T = {
    (arg +: args).reduce { (a: T, b: T) =>
      val ev = implicitly[Ordering[T]]
      ev.compare(a, b) match {
        case i if i < 0 => a
        case i if i > 0 => b
        case _          => a
      }
    }
  }
}

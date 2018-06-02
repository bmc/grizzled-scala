package grizzled.parsing

/** `SafeIterator` places a simple stream on top of an iterator,
  * returning `Option`-wrapped instances from the underlying iterator.
  * When the stream is exhausted, the `Iterator` stream returns
  * `None`. Differences from a plain `Iterator` include:
  *
  * - An `SafeIterator` will not throw an exception if you try to read
  *   past the end of it. Instead, it will just keep returning `None`.
  *
  * Example of use with a string:
  *
  * {{{
  * import grizzled.parsing.SafeIterator
  *
  * val s = ...
  * val istream = new SafeIterator[Char](s.elements)
  * }}}
  *
  * @param iterator  the iterator to wrap
  */
@SuppressWarnings(Array("org.wartremover.warts.Var"))
class SafeIterator[+T](private val iterator: Iterator[T]) {
  private var count = 0

  /** Alternate constructor that takes an `Iterable`.
    *
    * @param iterable the `Iterable`
    */
  def this(iterable: Iterable[T]) = this(iterable.iterator)

  /** Get the next item from the stream, advancing the cursor.
    *
    * @return an `Option` containing the next item, or `None`
    *         if the iterator is exhausted.
    */
  def next: Option[T] = {
    if (! iterator.hasNext)
      None

    else {
      count += 1
      Some(iterator.next)
    }
  }
}

/** Companion object for `SafeIterator`.
  */
object SafeIterator {
  /** Create a new `SafeIterator` from an `Iterable`.
    *
    * @param iterable  the `Iterable``
    * @tparam T        the type of the `Iterable`
    *
    * @return the allocated `SafeIterator`
    */
  def apply[T](iterable: Iterable[T]): SafeIterator[T] =
    new SafeIterator(iterable)

  /** Create a new `SafeIterator` from an `Iterator`.
    *
    * @param iterator  the `Iterator``
    * @tparam T        the type of the `Iterable`
    *
    * @return the allocated `SafeIterator`
    */
  def apply[T](iterator: Iterator[T]): SafeIterator[T] =
    new SafeIterator(iterator)
}

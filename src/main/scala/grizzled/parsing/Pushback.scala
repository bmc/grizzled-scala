package grizzled.parsing

/** The `Pushback` trait can be mixed into an `SafeIterator` to permit
  * arbitrary pushback.
  *
  * NOTE: This trait it not thread-safe.
  */
trait Pushback[T] extends SafeIterator[T] {

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var pushbackStack = List.empty[T]

  /** Get the next item from the stream, advancing the cursor, while
    * honoring previous calls to `pushback()`.
    *
    * @return an `Option` containing the next item, or `None`
    *         if the iterator is exhausted.
    */
  override def next: Option[T] = {

    pushbackStack match {
      case Nil => super.next

      case head :: tail =>
        pushbackStack = tail
        Some(head)
    }
  }

  /** Push a single item back onto the stream.
    *
    * @param item  the item
    */
  def pushback(item: T): Unit = {
    pushbackStack = item :: pushbackStack
  }

  /** Push a list of items back onto the stream. The items are pushed
    * back in reverse order, so the items in the list should be in the order
    * they were retrieved from the stream. For example:
    *
    * {{{
    * val stream = new SafeIterator[Char]("foobar") with Pushback[Char]
    * val list = List(stream.next.get, stream.next.get)
    *
    * // At this point, the list contains ('f', 'o'), and the stream
    * // contains "obar".
    *
    * stream.pushback(list) // Stream now contains "foobar"
    * }}}
    *
    * @param items  the items to push back.
    */
  def pushbackMany(items: List[T]): Unit = {
    pushbackStack = items ::: pushbackStack
  }
}

package grizzled.collection

/** An iterator that iterates, serially, over the contents of multiple other
  * iterators.
  *
  * '''This class is no longer necessary.''' Just use the `Iterator` operator
  * `++`, as outlined in
  * [[http://stackoverflow.com/a/16315251/53495 this StackOverflow answer]].
  *
  * @param iterators  the iterators to wrap
  */
@deprecated("Use Iterator++. See http://stackoverflow.com/a/16315251/53495", "2.3.2")
class MultiIterator[+T](iterators: Iterator[T]*) extends Iterator[T] {
  private[this] val iteratorList: List[Iterator[T]] = iterators.toList

  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.Var"))
  private[this] var current = iteratorList.head

  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.Var"))
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

package grizzled.collection

/** An iterator that iterates, serially, over the contents of multiple other
  * iterators.
  *
  * @param iterators  the iterators to wrap
  */
@SuppressWarnings(Array("org.brianmckenna.wartremover.warts.Var"))
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

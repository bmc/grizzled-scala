/**
 * Some collection-related helpers.
 */
package grizzled.collection

import java.util.{Collection, Iterator => JIterator}

/**
 * Useful for converting a collection into an object suitable for use with
 * Scala's <tt>for</tt> loop.
 */
class CollectionIterator[T](val iterator: JIterator[T]) extends Iterator[T]
{

    /**
     * Alternate constructor that takes a collection.
     *
     * @param collection  the collection
     */
    def this(collection: Collection[T]) = this(collection.iterator)

    def hasNext: Boolean = iterator.hasNext
    def next: T = iterator.next
}

object implicits
{
    implicit def javaCollectionToScalaIterator[T](c: Collection[T]) =
        new CollectionIterator[T](c)
}

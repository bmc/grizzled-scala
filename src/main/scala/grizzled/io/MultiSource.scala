package grizzled.io

import scala.io.Source
import scala.collection.compat._

/** A `MultiSource` contains multiple `scala.io.Source`
  * objects and satisfies reads from them serially. Once composed, a
  * `MultiSource` can be used anywhere a `Source` is used.
  *
  * @param sources  the sources to wrap
  */
class MultiSource(sources: List[Source]) extends Source {

  /** Version of constructor that takes multiple arguments, instead of a list.
    *
    * @param sources  the sources to wrap
    */
  def this(sources: Source*) = this(sources.toList)

  /** The actual iterator.
    */
  protected val iter: Iterator[Char] = {
    sources.map(_.iterator).foldLeft(Iterator[Char]())(_ ++ _)
  }

  /** Reset, returning a new source.
    */
  override def reset: Source = new MultiSource(sources.map(_.reset()))
}

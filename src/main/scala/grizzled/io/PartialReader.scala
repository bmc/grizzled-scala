package grizzled.io

import scala.annotation.tailrec

/** Contains methods that can read part of a stream or reader.
  */
trait PartialReader[T] {
  type HasRead = {def read(): Int}
  val reader: HasRead

  protected def convert(b: Int): T

  /** Read up to `max` items from the reader.
    *
    * @param max  maximum number of items to read
    *
    * @return a list of the items
    */
  def readSome(max: Int): List[T] = {
    import scala.language.reflectiveCalls

    @tailrec def doRead(r: HasRead, partialList: List[T], cur: Int): List[T] = {
      if (cur >= max)
        partialList

      else {
        val b = r.read()
        if (b == -1)
          partialList
        else
          doRead(r, partialList :+ convert(b), cur + 1)
      }
    }

    Option(reader).map(r => doRead(r, List.empty[T], 0)).getOrElse(Nil)
  }
}

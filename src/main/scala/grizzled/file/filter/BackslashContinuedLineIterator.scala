package grizzled.file.filter

import grizzled.string.Implicits.String._

import scala.io.Source
import scala.collection.mutable.ArrayBuffer
import scala.sys.SystemProperties

/** Assemble input lines, honoring backslash escapes for line continuation.
  * `BackslashContinuedLineIterator` takes an iterator over lines of
  * input, looks for lines containing trailing backslashes, and treats them
  * as continuation lines, to be concatenated with subsequent lines in the
  * input. Thus, when a `BackslashContinuedLineIterator` filters this
  * input:
  *
  * {{{
  * Lorem ipsum dolor sit amet, consectetur \
  * adipiscing elit.
  * In congue tincidunt fringilla. \
  * Sed interdum nibh vitae \
  * libero
  * fermentum id dictum risus facilisis.
  * }}}
  *
  * it produces these lines:
  *
  * {{{
  * Lorem ipsum dolor sit amet, consectetur adipiscing elit.
  * In congue tincidunt fringilla. Sed interdum nibh vitae libero
  * fermentum id dictum risus facilisis.
  * }}}
  *
  * @param source an iterator that produces lines of input. Any trailing
  *               newlines are stripped.
  */
class BackslashContinuedLineIterator(val source: Iterator[String])
extends Iterator[String] {
  private val lineSep = (new SystemProperties).getOrElse("line.separator", "\n")

  /** Determine whether there's any input remaining.
    *
    * @return `true` if input remains, `false` if not
    */
  def hasNext: Boolean = source.hasNext

  /** Get the next logical line of input, which may represent a concatenation
    * of physical input lines. Any trailing newlines are stripped.
    *
    * @return the next input line
    */
  def next: String = {

    import scala.annotation.tailrec

    @tailrec
    def readNext(buf: String): String = {
      if (! source.hasNext)
        buf
      else {
        source.next match {
          case line if line.lastOption == Some('\\') =>
            readNext(buf + line.dropRight(1))
          case line =>
            buf + line
        }
      }
    }

    readNext("")
  }
}

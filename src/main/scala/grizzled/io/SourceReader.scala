/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright © 2009-2016, Brian M. Clapper
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

  * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

  * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  * Neither the names "clapper.org", "Grizzled Scala Library", nor the
    names of its contributors may be used to endorse or promote products
    derived from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  ---------------------------------------------------------------------------
*/
package grizzled.io

import scala.io.Source
import java.io.{IOException, Reader}

import scala.annotation.tailrec

/** Provides a `java.io.Reader` that is backed by a Scala `Source` object.
  *
  * @param sourceToWrap  the source to wrap
  */
class SourceReader(sourceToWrap: Source) extends Reader {
  // A var is used only to support reset().
  private var source: Source = sourceToWrap

  /** Reads characters into a portion of an array. This method will block until
    * some input is available, an I/O error occurs, or the end of the
    * underlying `Source` is reached.
    *
    * @param buf     the destination character buffer
    * @param offset  offset at which to start reading into the buffer
    * @param length  maximum number of characters to read
    * @return total number of characters read, or -1 on EOF.
    */
  def read(buf: Array[Char], offset: Int, length: Int): Int = {
    @tailrec
    def readNext(i: Int, readSoFar: Int): Int = {
      if (readSoFar >= length)
        readSoFar
      else if (i >= buf.length)
        readSoFar
      else {
        read() match {
          case c if c <= 0 => readSoFar
          case c =>
            buf(i) = c.toChar
            readNext(i + 1, readSoFar + 1)
        }
      }
    }

    readNext(offset, 0)
  }

  /** Skips characters. This method will block until some characters are
    * available, an I/O error occurs, or the end of the underlying `Source`
    * is reached.
    *
    * @param n the number of characters to skip
    * @return the number of characters actually skipped
    */
  override def skip(n: Long): Long = {
    assert(n >= 0)

    @tailrec
    def skipNext(skippedSoFar: Long): Long = {
      if (skippedSoFar >= n)
        skippedSoFar
      else {
        read() match {
          case -1 => skippedSoFar
          case c  => skipNext(skippedSoFar + 1)
        }
      }
    }

    skipNext(0)
  }

  /** `mark()` is not supported. This method unconditionally throws
    * `IOException`.
    *
    * @param readAheadLimit the mark limit. Ignored.
    */
  override def mark(readAheadLimit: Int): Unit = {
    throw new IOException("mark() not supported.")
  }

  /** Tells whether the `Reader` is ready to be read. The `Reader` APi states
    * that this method "returns `true` if the next `read()` is guaranteed not to
 *
    * block for input, `false`` otherwise. Note that returning `false` does not
    * guarantee that the next read will block."
    *
    * There's no simple mapping of `ready()` to a `Source`, so this method
    * always returns `false`.
    *
    * @return `false`, unconditionally.
    */
  override def ready(): Boolean = true

  /** Reads a single character. This method will block until a character is
    * available, an I/O error occurs, or the end of the stream is reached.
    *
    * @return the character read, as an integer in the range 0x00 to 0xffff,
    *         or -1 if at the end of the underlying `Source`.
    */
  override def read(): Int = {
    try {
      source.next
    }
    catch {
      case e: NoSuchElementException => -1
    }
  }

  /** Resets the `Reader` by resetting the underlying `Source`.
    */
  override def reset(): Unit = {
    source = source.reset()
  }

  /** Closes the `Reader` by closing the underlying `Source`.
    */
  def close(): Unit = source.close()
}

/** Companion to `SourceReader` class.
  */
object SourceReader {
  /** Create a `SourceReader` from a `Source`. The result is compatible
    * with the `java.io.Reader` interface.
    *
    * @param source  the `scala.io.Source`
    *
    * @return the `SourceReader`
    */
  def apply(source: Source): SourceReader = new SourceReader(source)
}

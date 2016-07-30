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

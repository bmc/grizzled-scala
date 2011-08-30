/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2009, Brian M. Clapper
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

/** Contains various file- and I/O-related filter classes.
  */
package grizzled.file.filter

import grizzled.string.GrizzledString._

import scala.io.Source
import scala.collection.mutable.ArrayBuffer

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
  // Match this in reverse. Makes it easier for the backslashes to do a
  // greedy match.
  private val ReversedContinuedLine = """^(\\+)(.*)$""".r

  private val buf = new ArrayBuffer[Char]

  /** Alternate constructor that takes a `Source` object.
    *
    * @param source source from which to read lines
    */
  def this(source: Source) = this(source.getLines())

  /** Determine whether there's any input remaining.
    *
    * @return `true` if input remains, `false` if not
    */
  def hasNext: Boolean = source.hasNext || (buf.length > 0)

  /** Get the next logical line of input, which may represent a concatenation
    * of physical input lines. Any trailing newlines are stripped.
    *
    * @return the next input line
    */
  def next: String = {
    def makeLine(line: String): String = {
      if (buf.length > 0) {
        val res = (buf mkString "") + line
        buf.clear()
        res
      }

      else {
        line
      }
    }

    import scala.annotation.tailrec

    @tailrec def readNext: String = {
      if (! source.hasNext) {
        if (buf.length == 0)
          assert(false);
        val res = buf mkString ""
        buf.clear()
        res
      }

      else {
        val line = source.next.chomp // chomp() is in GrizzledString
        // Would like to use "match" here, but the ReversedContinuedLine
        // regex doesn't work as an extractor. I should figure out why...
        val m = ReversedContinuedLine.findFirstMatchIn(line.reverse)

        // Odd number of backslashes at the end of a line means
        // it's a continuation line.
        if (m == None)
          makeLine(line)

        else if ((m.get.group(1).length % 2) == 0)
          makeLine(line)

        else {
          buf ++= m.get.group(2).reverse
          readNext
        }
      }
    }

    readNext
  }
}

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

package grizzled.io

import scala.io.Source

/**
 * A wrapper for `java.io.File` that provides additional methods.
 * By importing the implicit conversion functions, you can use the methods
 * in this class transparently from a `java.io.File` object.
 *
 * {{{
 * import grizzled.io.GrizzledSource._
 *
 * val source = Source.fromFile(new java.io.File("/tmp/foo/bar"))
 * source.firstNonblankLine.getOrElse("")
 * }}}
 */
final class GrizzledSource(val source: Source)
{
    /**
     * Find the lines between two marker lines in the source. For instance,
     * to get all lines between the next occurrence of "{{{" (on a line by
     * itself and "}}}" (or end of file), use:
     *
     * {{{
     * import grizzled.io.GrizzledSource._
     * import scala.io.Source
     * import java.io.File
     *
     * val path = "/path/to/some/file"
     * val lines = Source.fromFile(new File(path)).linesBetween("{{{", "}}}")
     * }}}
     *
     * This method uses `Source.getLines()`, which may or may not start
     * at the beginning of the source, depending on the source's state.
     *
     * @param start  the starting line marker
     * @param finish    the ending line marker
     *
     * @return a iterator of lines, or an empty iterator if none found
     */
    def linesBetween(start: String, finish: String): Iterator[String] =
        source.getLines().dropWhile(_ != start).drop(1).takeWhile(_ != finish)

    /**
     * Find and return the first non-blank line (without trailing newline)
     * in the source. Uses `Source.getLines()`, which may or may not start
     * at the beginning of the source, depending on the source's state.
     *
     * @return `None` if there is no nonblank line, `Some(line)` if there is.
     */
    def firstNonblankLine: Option[String] =
    {
        source.getLines().dropWhile(_.trim.length == 0).take(1).toList match
        {
            case Nil          => None
            case line :: tail => Some(line)
        }
    }
}

/**
 * Companion object for `GrizzledSource`. To get implicit functions that
 * define automatic conversions between `GrizzledSource` and `scala.io.Source`,
 * import this module:
 *
 * {{{
 * import grizzled.io.GrizzledSource._
 * }}}
 */
object GrizzledSource
{
    implicit def sourceToGrizzledSource(s: Source): GrizzledSource =
        new GrizzledSource(s)

    implicit def grizzledSourceToSource(gs: GrizzledSource): Source = gs.source
}

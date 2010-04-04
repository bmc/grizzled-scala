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

/**
 * I/O-related classes and utilities. This package is distinguished from
 * the <tt>grizzled.file</tt> package in that this package operates on
 * already-open Java <tt>InputStream<tt>, <tt>OutputStream</tt>,
 * <tt>Reader</tt> and <tt>Writer</tt> objects, and on Scala
 * <tt>Source</tt> objects.
 */
package grizzled.io

import scala.io.Source

/**
 * A <tt>MultiSource</tt> contains multiple <tt>scala.io.Source</tt>
 * objects and satisfies reads from them serially. Once composed, a
 * <tt>MultiSource</tt> ahcan be used anywhere a <tt>Source</tt> is used.
 *
 * @param sources  the sources to wrap
 */
class MultiSource(sources: List[Source]) extends Source
{
    import grizzled.collection.MultiIterator

    private val sourceList = sources.toList

    /**
     * Version of constructor that takes multiple arguments, instead of a list.
     *
     * @param sources  the sources to wrap
     */
    def this(sources: Source*) = this(sources.toList)

    /**
     * The actual iterator.
     */
    protected val iter: Iterator[Char] = new MultiIterator[Char](sourceList: _*)

    /**
     * Reset, returning a new source.
     */
    override def reset: Source = new MultiSource(sourceList)
}

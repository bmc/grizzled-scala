/*---------------------------------------------------------------------------*\
  This software is released under a BSD-style license:

  Copyright (c) 2009 Brian M. Clapper. All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

  1.  Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.

  2.  The end-user documentation included with the redistribution, if any,
      must include the following acknowlegement:

        "This product includes software developed by Brian M. Clapper
        (bmc@clapper.org, http://www.clapper.org/bmc/). That software is
        copyright (c) 2009 Brian M. Clapper."

      Alternately, this acknowlegement may appear in the software itself,
      if wherever such third-party acknowlegements normally appear.

  3.  Neither the names "clapper.org", "The Grizzled Scala Library",
      nor any of the names of the project contributors may be used to
      endorse or promote products derived from this software without prior
      written permission. For written permission, please contact
      bmc@clapper.org.

  4.  Products derived from this software may not be called "clapper.org
      Java Utility Library", nor may "clapper.org" appear in their names
      without prior written permission of Brian M. Clapper.

  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
  NO EVENT SHALL BRIAN M. CLAPPER BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
\*---------------------------------------------------------------------------*/

/**
 * Contains various file- and I/O-related filter classes.
 */
package grizzled.file.filter

import grizzled.string.implicits._

import scala.io.Source

/**
 * Assemble input lines, honoring backslash escapes for line continuation.
 * <tt>BackslashContinuedLineIterator</tt> takes an iterator over lines of
 * input, looks for lines containing trailing backslashes, and treats them
 * as continuation lines, to be concatenated with subsequent lines in the
 * input. Thus, when a <tt>BackslashContinuedLineIterator</tt> filters this
 * input:
 *
 * <blockquote><pre>
 * Lorem ipsum dolor sit amet, consectetur \
 * adipiscing elit.
 * In congue tincidunt fringilla. \
 * Sed interdum nibh vitae \
 * libero
 * fermentum id dictum risus facilisis.
 * </pre></blockquote>
 *
 * it produces these lines:
 * 
 * <blockquote><pre>
 * Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * In congue tincidunt fringilla. Sed interdum nibh vitae libero
 * fermentum id dictum risus facilisis.
 * </pre></blockquote>
 *
 * @param source an iterator that produces lines of input. Any trailing newlines
 *               are stripped.
 */
class BackslashContinuedLineIterator(val source: Iterator[String])
    extends Iterator[String]
{
    // Match this in reverse. Makes it easier for the backslashes to do a
    // greedy match.
    private val ReversedContinuedLine = """^(\\+)(.*)$""".r

    /**
     * Alternate constructor that takes a <tt>Source</tt> object.
     *
     * @param source source from which to read lines
     */
    def this(source: Source) = this(source.getLines)

    /**
     * Determine whether there's any input remaining.
     *
     * @return <tt>true</tt> if input remains, <tt>false</tt> if not
     */
    def hasNext: Boolean = source.hasNext

    /**
     * Get the next logical line of input, which may represent a concatenation
     * of physical input lines. Any trailing newlines are stripped.
     *
     * @return the next input line
     */
    def next: String =
    {
        import scala.collection.mutable.ArrayBuffer

        val buf = new ArrayBuffer[Char]

        def makeLine(line: String): String =
            if (buf.length > 0)
            {
                val res = (buf mkString "") + line
                buf.clear
                res
            }

            else
            {
                line
            }

        def readNext: String =
        {
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

            else
            {
                buf ++= m.get.group(2).reverse
                readNext
            }
        }

        readNext
    }
}

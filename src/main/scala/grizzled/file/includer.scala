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

package grizzled.file

import scala.io.Source
import scala.util.matching.Regex

import java.net.URI

/**
 * Process <i>include</i> directives in files, returning an iterator over
 * lines from the flattened files.
 *
 * <p>The <tt>grizzled.file.Includer</tt> class can be used to process
 * <i>includes</i> within a text file, returning a file-like object. It
 * also contains some utility functions that permit using include-enabled
 * files in other contexts.</p>
 * 
 * <h3>Syntax</h3>
 *
 * <p>The <i>include</i> syntax is defined by a regular expression; any
 * line that matches the regular expression is treated as an <i>include</i>
 * directive. The default regular expression, <tt>^%include\s"([^"]+)"$</tt>
 * matches include directives like this:</p>
 *
 * <blockquote><pre>
 * %include "/absolute/path/to/file"
 * %include "../relative/path/to/file"
 * %include "local_reference"
 * %include "http://localhost/path/to/my.config"
 * </pre></blockquote>
 * 
 * <p>Relative and local file references are relative to the including file
 * or URL. That is, if an <tt>Includer</tt> is processing file
 * "/home/bmc/foo.txt" and encounters an attempt to include file "bar.txt",
 * it will assume "bar.txt" is to be found in "/home/bmc".</p>
 *
 * <p>Similarly, if an <tt>Includer</tt> is processing URL
 * "http://localhost/bmc/foo.txt" and encounters an attempt to include file
 * "bar.txt", it will assume "bar.txt" is to be found at
 * "http://localhost/bmc/bar.txt".</p>
 *
 * <p>Nested includes are permitted; that is, an included file may, itself,
 * include other files. The maximum recursion level is configurable and
 * defaults to 100.</p>
 * 
 * <p>The include syntax can be changed by passing a different regular
 * expression to the <tt>Includer</tt> constructor.</p>
 * 
 * <h3>Usage</h3>
 *
 * <p>This package provides an <tt>Includer</tt> class, which processes
 * include directives in a file and behaves somewhat like a Scala
 * <tt>Source</tt>. See the class documentation for more details.</p>
 * 
 * <p>The package also provides a <tt>preprocess()</tt> convenience
 * function, via a companion object, that can be used to preprocess a file;
 * it returns the path to the resulting preprocessed file.</p>
 * 
 * <h3>Examples</h3>
 * 
 * <p>Preprocess a file containing include directives, then read the result:</p>
 * 
 * <blockquote><pre>
 * import grizzled.file.includer.Includer
 * 
 * val inc = Includer(path)
 * for (line <- inc.getLines)
 *     println(line)
 * </pre></blockquote>
 * 
 * <p>Use an include-enabled file with a Scala <tt>scala.io.Source</tt>
 * object:</p>
 * 
 * <blockquote><pre>
 * import grizzled.file.includer.Includer
 * import scala.io.Source
 * 
 * val source = Source.fromFile(Includer.preprocess("/path/to/file"))
 * </pre></blockquote>
 *
 * @param source       the <tt>Source</tt> to read
 * @param includeRegex the regular expression that defines an include directive.
 *                     Must contain a group that surrounds the file or URL part.
 * @param maxNesting   the maximum nesting level
 */
class Includer(val source: Source,
               val includeRegex: Regex,
               val maxNesting: Int)
    extends Iterator[String]
{
    import scala.collection.mutable.Stack

    /**
     * Used to maintain the stack of sources being read.
     */
    private class IncludeSource(val source: Source)
    {
        val iterator = source.getLines
    }

    /**
     * The stack of sources being read.
     */
    private val sourceStack = new Stack[IncludeSource]

    sourceStack.push(new IncludeSource(source))

    /**
     * Determine whether there are any more input lines to be read from the
     * includer.
     *
     * @return <tt>true</tt> if at least one more line is available,
     *         <tt>false</tt> otherwise
     */
    def hasNext: Boolean =
    {
        def somethingHasNext(stack: List[IncludeSource]): Boolean =
        {
            if (stack.length == 0)
                false
            else if (stack.head.iterator.hasNext)
                true
            else
                somethingHasNext(stack.tail)
        }

        somethingHasNext(sourceStack.toList.reverse)
    }

    /**
     * Get the next input line. You should call <tt>hasNext</tt> before calling
     * this method, to ensure that there are input lines remaining.
     *
     * @return the next input line
     *
     * @throws IllegalStateException no more input lines
     */
    def next: String =
    {
        def nextFromStack: String =
        {
            if (sourceStack.length == 0)
                throw new IllegalStateException("No more data")

            if (sourceStack.top.iterator.hasNext)
                sourceStack.top.iterator.next
            else
            {
                sourceStack.pop
                nextFromStack
            }
        }

        import grizzled.file.{util => futil}

        val line = nextFromStack
        includeRegex.findFirstMatchIn(line) match
        {
            case None =>
                if (line.endsWith("\n"))
                    line.substring(0, line.length - 1)
                else
                    line

            case Some(incMatch) =>
                if (sourceStack.length >= maxNesting)
                    throw new IllegalStateException("Max nesting level (" +
                                                    maxNesting + ") " +
                                                    "exceeded.")

                val curURI =  new URI(sourceStack.top.source.descr)
                val path = futil.joinPath(futil.dirname(curURI.getPath),
                                          incMatch.group(1))
                val newURI = new URI(curURI.getScheme,
                                     curURI.getHost,
                                     path,
                                     curURI.getFragment)
                sourceStack.push(new IncludeSource(Source.fromFile(newURI)))
                next
        }
    }
}

/**
 * Companion object for the <tt>Includer</tt> class. Also contains some
 * utility methods, such as the <tt>preprocess()</tt> method.
 */
object Includer
{
    /**
     * The default regular expression for matching include directives.
     */
    val DefaultIncludeRegex = "^%include\\s\"([^\"]+)\"$".r

    /**
     * The default maximum nesting level for includes.
     */
    val DefaultMaxNesting   = 100

    /**
     * Allocate an includer.
     *
     * @param source       the <tt>Source</tt> to read
     * @param includeRegex the regular expression that defines an include
     *                     directive. Must contain a group that surrounds the
     *                     file or URL part.
     * @param maxNesting   the maximum nesting level
     */
    def apply(source: Source, includeRegex: Regex, maxNesting: Int): Includer =
        new Includer(source, includeRegex, maxNesting)

    /**
     * Allocate an includer, using the default value for the
     * <tt>maxNesting</tt> parameter.
     *
     * @param source       the <tt>Source</tt> to read
     * @param includeRegex the regular expression that defines an include
     *                     directive. Must contain a group that surrounds the
     *                     file or URL part.
     */
    def apply(source: Source, includeRegex: Regex): Includer = 
        apply(source, includeRegex, DefaultMaxNesting)

    /**
     * Allocate an includer, using the default values for the
     * <tt>maxNesting</tt> and <tt>includeRegex</tt> parameters.
     *
     * @param source       the <tt>Source</tt> to read
     */
    def apply(source: Source): Includer = 
        apply(source, DefaultIncludeRegex, DefaultMaxNesting)

    /**
     * Allocate an includer.
     *
     * @param pathOrURI    the path or URI string to read
     * @param includeRegex the regular expression that defines an include
     *                     directive. Must contain a group that surrounds the
     *                     file or URL part.
     * @param maxNesting   the maximum nesting level
     */
    def apply(pathOrURI: String, 
              includeRegex: Regex, 
              maxNesting: Int): Includer =
    {
        import grizzled.file.{util => futil}
        import java.io.File
        import java.net.URISyntaxException

        def getURI: URI =
        {
            if (pathOrURI.startsWith(futil.fileSeparator))
                new File(pathOrURI).toURI

            else
            {
                // Windows paths (with backslashes) will cause a 
                // URISyntaxException, so we can use that behavior to check
                // for them.

                try
                {
                    new URI(pathOrURI)
                }

                catch
                {
                    case _: URISyntaxException => new File(pathOrURI).toURI
                }
            }
        }
            
        Includer(Source.fromFile(getURI), includeRegex, maxNesting)
    }

    /**
     * Allocate an includer, using the default value for the
     * <tt>maxNesting</tt> parameter.
     *
     * @param pathOrURI    the path or URI string to read
     * @param source       the <tt>Source</tt> to read
     * @param includeRegex the regular expression that defines an include
     *                     directive. Must contain a group that surrounds the
     *                     file or URL part.
     */
    def apply(pathOrURI: String, includeRegex: Regex): Includer = 
        apply(pathOrURI, includeRegex, DefaultMaxNesting)

    /**
     * Allocate an includer, using the default values for the
     * <tt>maxNesting</tt> and <tt>includeRegex</tt> parameters.
     *
     * @param pathOrURI    the path or URI string to read
     */
    def apply(pathOrURI: String): Includer = 
        apply(pathOrURI, DefaultIncludeRegex, DefaultMaxNesting)

    /**
     * Process all include directives in the specified file, returning a
     * path to a temporary file that contains the results of the expansion.
     * The temporary file is automatically removed when the program exits,
     * though the caller is free to remove it whenever it is no longer
     * needed.
     *
     * @param pathOrURI   the path or URI string to read
     * @param tempPrefix  temporary file prefix, with the same meaning as the
     *                    temporary file prefix used by
     *                    <tt>java.io.File.createTempFile()</tt>
     * @param tempSuffix  temporary file suffix, with the same meaning as the
     *                    temporary file suffix used by
     *                    <tt>java.io.File.createTempFile()</tt>
     *
     * @return the path to the temporary file
     */
    def preprocess(pathOrURI:  String, 
                   tempPrefix: String, 
                   tempSuffix: String): String =
    {
        import java.io.{File, FileWriter}

        val includer = Includer(Source.fromFile(pathOrURI))
        val fileOut = File.createTempFile(tempPrefix, tempSuffix)
        fileOut.deleteOnExit
        val out = new FileWriter(fileOut)
        for (line <- includer)
            out.write(line)
        out.close
        fileOut.getAbsolutePath
    }
}

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

import java.io.{InputStream,
                OutputStream,
                Reader,
                Writer}

/**
 * Contains methods that can read part of a stream or reader.
 */
trait PartialReader[T]
{
    val reader: {def read(): Int}

    protected def convert(b: Int): T

    /**
     * Read up to <tt>max</tt> items from the reader.
     *
     * @param max  maximum number of items to read
     *
     * @return a list of the items
     */
    def readSome(max: Int): List[T] =
    {
        def doRead(cur: Int): List[T] =
        {
            if (cur >= max)
                Nil

            else
            {
                val b = reader.read()
                if (b == -1)
                    Nil
                else
                    convert(b) :: doRead(cur + 1)
            }
        }

        if (reader == null)
            Nil
        else
            doRead(0)
    }
}

/**
 * Provides additional methods, over and above those already present in
 * the Java <tt>Reader</tt> class. The <tt>implicits</tt> object
 * contains implicit conversions between <tt>RichReader</tt> and
 * <tt>Reader</tt>.
 *
 * @param reader  the input stream to wrap
 */
class RichReader(val reader: Reader) extends PartialReader[Char]
{
    protected def convert(b: Int) = b.asInstanceOf[Char]

    /**
     * Copy the input stream to an output stream, stopping on EOF. This
     * method does no buffering. If you want buffering, make sure you use a
     * <tt>java.io.BufferedReader</tt> and a <tt>java.io.BufferedWriter.
     * This method does not close either object.
     *
     * @param out  the output stream
     */
    def copyTo(out: Writer): Unit =
    {
        val c: Int = reader.read()
        if (c != -1)
        {
            out.write(c)
            // Tail recursion means never having to use a var.
            copyTo(out)
        }
    }
}

/**
 * Provides additional methods, over and above those already present in
 * the Java <tt>InputStream</tt> class. The <tt>implicits</tt> object
 * contains implicit conversions between <tt>RichInputStream</tt> and
 * <tt>InputStream</tt>.
 *
 * @param inputStream  the input stream to wrap
 */
class RichInputStream(val inputStream: InputStream) extends PartialReader[Byte]
{
    val reader = inputStream

    protected def convert(b: Int) = b.asInstanceOf[Byte]

    /**
     * Copy the input stream to an output stream, stopping on EOF. This
     * method does no buffering. If you want buffering, make sure you use a
     * <tt>java.io.BufferedInputStream</tt> and a
     * <tt>java.io.BufferedOutputStream</tt>. This method does not close
     * either stream.
     *
     * @param out  the output stream
     */
    def copyTo(out: OutputStream): Unit =
    {
        def recCopyTo()
        {
            val c: Int = inputStream.read()
            if (c != -1)
            {
                out.write(c)
                // Tail recursion means never having to use a var.
                recCopyTo()
            }
        }

        recCopyTo()
    }
}

/**
 * A <tt>MultiSource</tt> contains multiple <tt>scala.io.Source</tt>
 * objects and satisfies reads from them serially. Once composed, a
 * <tt>MultiSource</tt> ahcan be used anywhere a <tt>Source</tt> is used.
 *
 * @param sources  the sources to wrap
 */
class MultiSource(sources: List[Source]) extends Source
{
    import collection.MultiIterator

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
    def reset: Source = new MultiSource(sourceList)
}

/**
 * Some utility methods.
 */
object util
{
    type Closeable = {def close(): Unit}

    /**
     * <p>Ensure that a closeable object is closed. Note that this function
     * uses a Scala structural type, rather than a <tt>java.io.Closeable</tt>,
     * because there are classes and interfaces (e.g.,
     * <tt>java.sql.ResultSet</tt>) that have <tt>close()</tt> methods that do
     * not extend or implement <tt>java.io.Closeable</tt>.</p>
     *
     * Sample use:
     *
     * <blockquote><pre>
     * val in = new java.io.FileInputStream("/path/to/file")
     * useThenClose(in)
     * {
     *     ...
     * }
     * </pre></blockquote>
     *
     * The closeable object is not passed into the block, because its type
     * is useless to the block.
     *
     * @param closeable  the object that implements <tt>Closeable</tt>
     * @param block      the code block to execute with the <tt>Closeable</tt>
     *
     * @return whatever the block returns
     */
    def useThenClose[T](closeable: Closeable)(block: => T) =
    {
        try
        {
            block
        }

        finally
        {
            closeable.close
        }
    }
}

/**
 * Implicit conversions between <tt>RichInputStream</tt> and
 * <tt>InputStream</tt> objects.
 */
object implicits
{
    implicit def inputStreamToRichInputStream(inputStream: InputStream) =
        new RichInputStream(inputStream)

    implicit def richInputStreamInputStream(richInputStream: RichInputStream) =
        richInputStream.inputStream

    implicit def readerToRichReader(reader: Reader) = new RichReader(reader)

    implicit def richReaderToReader(richReader: RichReader) = richReader.reader
}

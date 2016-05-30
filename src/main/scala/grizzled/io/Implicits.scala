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

import java.io.{InputStream, OutputStream, Reader, Writer}

import scala.annotation.tailrec
import scala.io.Source
import scala.util.{Failure, Success, Try}

/** Implicits that addFile enrichments to `java.io` and `scala.io` classes.
  */
object Implicits {

  /** Provides additional methods, over and above those already present in
  * the Java `InputStream` class. The `implicits` object contains implicit
  * conversions between `RichInputStream` and `InputStream`.
  *
  * @param inputStream  the input stream to wrap
  */
  implicit class RichInputStream(val inputStream: InputStream)
    extends PartialReader[Byte] {

    val reader = inputStream

    protected def convert(b: Int) = (b & 0xff).toByte

    /** Copy the input stream to an output stream, stopping on EOF.
      * This method does not close either stream.
      *
      * @param out  the output stream
      */
    def copyTo(out: OutputStream): Try[Int] = {
      val buffer = new Array[Byte](8192)

      @tailrec
      def doCopyTo(copiedSoFar: Int): Try[Int] = {
        Try { reader.read(buffer) } match {
          case Success(n) if n <= 0 => Success(copiedSoFar)
          case Success(n) => Try { out.write(buffer, 0, n) } match {
            case Failure(ex) => Failure(ex)
            case Success(_)  => doCopyTo(copiedSoFar + n)
          }
        }
      }

      doCopyTo(0)
    }
  }

  /** Provides additional methods, over and above those already present in
    * the Java `Reader` class. The `implicits` object contains implicit
    * conversions between `RichReader` and `Reader`.
    *
    * @param reader  the reader to wrap
    */
  implicit class RichReader(val reader: Reader) extends PartialReader[Char] {
    protected def convert(b: Int) = (b & 0xff).toChar

    /** Copy the reader to a writer, stopping on EOF. This method does no
      * buffering. If you want buffering, make sure you use a
      * `java.io.BufferedReader` and a `java.io.BufferedWriter`. This method
      * does not close either object.
      *
      * @param out  the output stream
      */
    def copyTo(out: Writer): Unit = {
      val buffer = new Array[Char](8192)
      @tailrec
      def doCopyTo(copiedSoFar: Int): Try[Int] = {
        Try { reader.read(buffer) } match {
          case Success(n) if n <= 0 => Success(copiedSoFar)
          case Success(n) => Try { out.write(buffer, 0, n) } match {
            case Failure(ex) => Failure(ex)
            case Success(_)  => doCopyTo(copiedSoFar + n)
          }
        }
      }

      doCopyTo(0)
    }
  }

  /** A wrapper for `scala.io.Source` that provides additional methods.
    * By importing the implicit conversion functions, you can use the methods
    * in this class transparently from a `java.io.File` object.
    *
    * {{{
    * import grizzled.io.Implicits._
    *
    * val source = Source.fromFile(new java.io.File("/tmp/foo/bar"))
    * source.firstNonblankLine.getOrElse("")
    * }}}
    */
  implicit class GrizzledSource(val source: Source) {

    /** Find the lines between two marker lines in the source. For instance,
      * to get all lines between the next occurrence of "{{{" (on a line by
      * itself and "}}}" (or end of file), use:
      *
      * {{{
      * import grizzled.io.Implicits._
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
      source.getLines.dropWhile(_ != start).drop(1).takeWhile(_ != finish)

    /** Find and return the first non-blank line (without trailing newline)
      * in the source. Uses `Source.getLines()`, which may or may not start
      * at the beginning of the source, depending on the source's state.
      *
      * @return `None` if there is no nonblank line, `Some(line)` if there is.
      */
    def firstNonblankLine: Option[String] = {
      source.getLines.dropWhile(_.trim.length == 0).take(1).toSeq.headOption
    }
  }
}

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
import scala.annotation.tailrec
import scala.language.implicitConversions

import java.io.{Reader, Writer}

/** Provides additional methods, over and above those already present in
  * the Java `Reader` class. The `implicits` object contains implicit
  * conversions between `RichReader` and `Reader`.
  *
  * @param reader  the reader to wrap
  */
class RichReader(val reader: Reader) extends PartialReader[Char] {
  protected def convert(b: Int) = b.asInstanceOf[Char]

  /** Copy the reader to a writer, stopping on EOF. This method does no
    * buffering. If you want buffering, make sure you use a
    * `java.io.BufferedReader` and a `java.io.BufferedWriter. This method
    * does not close either object.
    *
    * @param out  the output stream
    */
  def copyTo(out: Writer): Unit = {
    @tailrec def doCopyTo: Unit = {
      val c: Int = reader.read()
      if (c != -1) {
        out.write(c)
        // Tail recursion means never having to use a var.
        doCopyTo
      }
    }

    doCopyTo
  }
}

/** Companion object to `RichReader` class. Importing this object brings the
  * implicit conversations into scope.
  */
object RichReader {
  implicit def readerToRichReader(reader: Reader) = new RichReader(reader)
  implicit def richReaderToReader(richReader: RichReader) = richReader.reader
}

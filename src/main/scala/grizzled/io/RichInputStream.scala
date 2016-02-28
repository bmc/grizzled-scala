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

import scala.annotation.tailrec
import scala.language.implicitConversions

import java.io.{InputStream, OutputStream}

/** Provides additional methods, over and above those already present in
  * the Java `InputStream` class. The `implicits` object contains implicit
  * conversions between `RichInputStream` and `InputStream`.
  *
  * @param inputStream  the input stream to wrap
  */
class RichInputStream(val inputStream: InputStream)
extends PartialReader[Byte] {
  val reader = inputStream

  protected def convert(b: Int) = b.asInstanceOf[Byte]

  /** Copy the input stream to an output stream, stopping on EOF.
    * This method does not close either stream.
    *
    * @param out  the output stream
    */
  def copyTo(out: OutputStream): Unit = {
      val buffer = new Array[Byte](8192)

      @tailrec def doCopyTo(): Unit = {
          val read = reader.read(buffer)
          if (read > 0) {
            out.write(buffer, 0, read)
            // Tail recursion means never having to use a var.
            doCopyTo()
          }
        }

      doCopyTo()
    }
}

/** Companion object to `RichInputStream` class. Importing this object brings
  * the implicit conversations into scope.
  */
object RichInputStream {
  implicit def isToRichInputStream(inputStream: InputStream): RichInputStream =
    new RichInputStream(inputStream)

  implicit def richInputStreamIS(richInputStream: RichInputStream): InputStream =
    richInputStream.inputStream
}

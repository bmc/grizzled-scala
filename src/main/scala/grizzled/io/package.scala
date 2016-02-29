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

package grizzled

/** I/O-related classes and utilities. This package is distinguished from
  * the `grizzled.file` package in that this package operates on
  * already-open Java `InputStream`, `OutputStream`, `Reader` and `Writer`
  * objects, and on Scala `Source` objects.
  *
  * See [[grizzled.file]]
  */
package object io {
  import scala.language.reflectiveCalls

  /** Ensure that a closeable object is closed. Note that this function
    * uses a Scala structural type, rather than a `java.io.Closeable`,
    * because there are classes and interfaces (e.g., `java.sql.ResultSet`)
    * that have `close()` methods that do not extend or implement
    * `java.io.Closeable`.
    *
    * Sample use:
    *
    * {{{
    * withCloseable(new java.io.FileInputStream("/path/to/file")) {
    *     in => ...
    * }
    * }}}
    *
    * The closeable object is not passed into the block, because its type
    * is useless to the block.
    *
    * @param closeable  the object that implements `Closeable`
    * @param code       the code block to execute with the `Closeable`
    *
    * @return whatever the block returns
    */
  def withCloseable[T <: {def close(): Unit}, R](closeable: T)(code: T => R) = {
    try {
      code(closeable)
    }

    finally {
      closeable.close()
    }
  }
}

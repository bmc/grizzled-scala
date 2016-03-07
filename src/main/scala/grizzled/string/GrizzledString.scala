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

package grizzled.string

/** Companion object for `GrizzledString`. To get implicit functions that
  * define automatic conversions between `GrizzledString` and `String`,
  * import this module:
  *
  * {{{
  * import grizzled.io.GrizzledString._
  * }}}
  */
object GrizzledString {
  import scala.collection.immutable.{StringOps, StringLike}
  import scala.language.implicitConversions
  import grizzled.string.Implicits.String.GrizzledString

  /** Implicit function to convert from a Java string to a
    * `GrizzledString`.
    *
    * @param s  the Java string
    *
    * @return the `GrizzledString`
    */
  @deprecated("Please import grizzled.string.Implicits.String._", "1.5.1")
  implicit def JavaString_GrizzledString(s: String) = new GrizzledString(s)

  /** Implicit function to convert from a `GrizzledString` to a
    * Java string.
    *
    * @param gs  the `GrizzledString`
    *
    * @return the Java string
    */
  @deprecated("Please import grizzled.string.Implicits.String._", "1.5.1")
  implicit def GrizzledString_JavaString(gs: GrizzledString) = gs.string

  /** Implicit function to convert from a Scala string object to a
    * `GrizzledString`.
    *
    * @param rs  a Scala string
    *
    * @return the `GrizzledString`
    */
  @deprecated("Please import grizzled.string.Implicits.String._", "1.5.1")
  implicit def ScalaString_GrizzledString(rs: StringLike[String]) =
    new GrizzledString(rs.toString)

  /** Implicit function to convert from `GrizzledString` to a
    * Scala `RichString`.
    *
    * @param gs  the `GrizzledString`
    *
    * @return the Scala string
    */
  @deprecated("Please import grizzled.string.Implicits.String._", "1.5.1")
  implicit def GrizzledString_ScalaString(gs: GrizzledString) =
    new StringOps(gs.string)
}

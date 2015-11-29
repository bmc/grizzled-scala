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

import scala.util.matching.Regex
import scala.language.implicitConversions

/** An analog to Scala's `RichChar` class, providing some methods
  * that neither `RichChar` nor `Char` (nor, for that matter,
  * `java.lang.Character`) provide. By importing the implicit conversion
  * functions, you can use the methods in this class transparently from a
  * `Char`, `RichChar` or `Character` object.
  *
  * {{{
  * import grizzled.string.implicits._
  * val ch = 'a'
  * println(ch.isHexDigit) // prints: true
  * }}}
  */
final class GrizzledChar(val character: Char) {
  /** Determine whether the character represents a valid hexadecimal
    * digit. This is a specialization of `isDigit(radix)`.
    *
    * @return `true` if the character is a valid hexadecimal
    *         digit, `false` if not.
    */
  def isHexDigit = isDigit(16)

  /** Determine whether the character represents a valid digit in a
    * given base.
    *
    * @param radix the radix
    *
    * @return `true` if the character is a valid digit in the
    *         indicated radix, `false` if not.
    */
  def isDigit(radix: Int): Boolean = {
    try {
      Integer.parseInt(character.toString, radix)
      true
    }
    catch {
      case _: NumberFormatException => false
    }
  }
}

/** Companion object for `GrizzledChar`. To get implicit functions that
  * define automatic conversions between `GrizzledChar` and `Char`,
  * import this module:
  *
  * {{{
  * import grizzled.io.GrizzledChar._
  * }}}
  */
object GrizzledChar {
  import scala.runtime.RichChar

  /** Implicit function to convert from a character to a `GrizzledChar`.
    *
    * @param s  the character
    *
    * @return the `GrizzledChar`
    */
  implicit def Char_GrizzledChar(c: Char) = new GrizzledChar(c)

  /** Implicit function to convert from a `GrizzledChar` to a
    * character.
    *
    * @param gc  the `GrizzledChar`
    *
    * @return the character
    */
  implicit def GrizzledChar_Char(gc: GrizzledChar) = gc.character

  /** Implicit function to convert from a Java `Character` object
    * to a `GrizzledChar`.
    *
    * @param s  the `Character` object
    *
    * @return the `GrizzledChar`
    */
  implicit def JavaCharacter_GrizzledChar(c: java.lang.Character) =
    new GrizzledChar(c.charValue)

  /** Implicit function to convert from a `GrizzledChar` to a
    * Java `Character` object.
    *
    * @param gc  the `GrizzledChar`
    *
    * @return the `Character` object
    */
  implicit def GrizzledChar_JavaCharacter(c: GrizzledChar) =
    new java.lang.Character(c.character)


  /** Implicit function to convert from a Scala `RichChar` object
    * to a `GrizzledChar`.
    *
    * @param s  the `RichChar` object
    *
    * @return the `GrizzledChar`
    */
  implicit def RichChar_GrizzledChar(c: RichChar) =
    new GrizzledChar(c.self.asInstanceOf[Char])

  /** Implicit function to convert from a `GrizzledChar` to a
    * Scala `RichChar` object.
    *
    * @param gc  the `GrizzledChar`
    *
    * @return the `RichChar` object
    */
  implicit def GrizzledChar_RichChar(c: GrizzledChar) =
    new RichChar(c.character)
}

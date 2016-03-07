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

object GrizzledChar {
  import scala.runtime.RichChar
  import scala.language.implicitConversions
  import grizzled.string.Implicits.Char.GrizzledChar

  /** Implicit function to convert from a character to a `GrizzledChar`.
    *
    * @param c  the character
    *
    * @return the `GrizzledChar`
    */
  @deprecated("Please import grizzled.string.Implicits.Character._", "1.5.1")
  implicit def Char_GrizzledChar(c: Char) = new GrizzledChar(c)

  /** Implicit function to convert from a `GrizzledChar` to a
    * character.
    *
    * @param gc  the `GrizzledChar`
    *
    * @return the character
    */
  @deprecated("Please import grizzled.string.Implicits.Character._", "1.5.1")
  implicit def GrizzledChar_Char(gc: GrizzledChar) = gc.character

  /** Implicit function to convert from a Java `Character` object
    * to a `GrizzledChar`.
    *
    * @param c  the `Character` object
    *
    * @return the `GrizzledChar`
    */
  @deprecated("Please import grizzled.string.Implicits.Character._", "1.5.1")
  implicit def JavaCharacter_GrizzledChar(c: java.lang.Character) =
    new GrizzledChar(c.charValue)

  /** Implicit function to convert from a `GrizzledChar` to a
    * Java `Character` object.
    *
    * @param c  the `GrizzledChar`
    *
    * @return the `Character` object
    */
  @deprecated("Please import grizzled.string.Implicits.Character._", "1.5.1")
  implicit def GrizzledChar_JavaCharacter(c: GrizzledChar) =
    new java.lang.Character(c.character)


  /** Implicit function to convert from a Scala `RichChar` object
    * to a `GrizzledChar`.
    *
    * @param c  the `RichChar` object
    *
    * @return the `GrizzledChar`
    */
  @deprecated("Please import grizzled.string.Implicits.Character._", "1.5.1")
  implicit def RichChar_GrizzledChar(c: RichChar) =
    new GrizzledChar(c.self.asInstanceOf[Char])

  /** Implicit function to convert from a `GrizzledChar` to a
    * Scala `RichChar` object.
    *
    * @param c  the `GrizzledChar`
    *
    * @return the `RichChar` object
    */
  @deprecated("Please import grizzled.string.Implicits.Character._", "1.5.1")
  implicit def GrizzledChar_RichChar(c: GrizzledChar) =
    new RichChar(c.character)
}

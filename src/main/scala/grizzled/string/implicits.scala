/*
  ---------------------------------------------------------------------------
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
  ---------------------------------------------------------------------------
*/

package grizzled.string

import scala.runtime.{RichChar, RichString}

/**
 * Miscellaneous implicit string conversions.
 */
object implicits
{
    /**
     * An implicit conversion that handles creating a Boolean from a string
     * value. This implicit definition, when in scope, allows code like
     * the following:
     *
     * <blockquote><pre>
     * val flag: Boolean = "true" // implicitly converts "true" to <tt>true</tt>
     * </pre></blockquote>
     *
     * This method currently understands the following strings (in any mixture
     * of upper and lower case). It is currently English-specific.
     *
     * <blockquote>true, t, yes, y, 1<br>false, f, no, n, 0</blockquote>
     *
     * @param s  the string to convert
     *
     * @return a boolean value
     *
     * @throws IllegalArgumentException if <tt>s</tt> cannot be parsed
     */
    implicit def bool(s: String): Boolean =
        s.trim.toLowerCase match
        {
            case "true"  => true
            case "t"     => true
            case "yes"   => true
            case "y"     => true
            case "1"     => true
            case "on"    => true

            case "false" => false
            case "f"     => false
            case "no"    => false
            case "n"     => false
            case "0"     => false
            case "off"   => false

            case _       => 
                throw new IllegalArgumentException("Can't convert string \"" +
                                                   s + "\" to boolean.")
        }

    /**
     * Implicit function to convert from a Java string to a
     * <tt>GrizzledString</tt>.
     *
     * @param s  the Java string
     *
     * @return the <tt>GrizzledString</tt>
     */
    implicit def JavaString_GrizzledString(s: String) = new GrizzledString(s)

    /**
     * Implicit function to convert from a <tt>GrizzledString</tt> to a
     * Java string.
     *
     * @param gs  the <tt>GrizzledString</tt>
     *
     * @return the Java string
     */
    implicit def GrizzledString_JavaString(gs: GrizzledString) = gs.string

    /**
     * Implicit function to convert from a Scala <tt>RichString</tt> to a
     * <tt>GrizzledString</tt>.
     *
     * @param s  a Scala <tt>RichString</tt>
     *
     * @return the <tt>GrizzledString</tt>
     */
    implicit def RichString_GrizzledString(rs: RichString) =
        new GrizzledString(rs.self)

    /**
     * Implicit function to convert from <tt>GrizzledString</tt> to a
     * Scala <tt>RichString</tt>.
     *
     * @param s  the <tt>GrizzledString</tt>
     *
     * @return the Scala <tt>RichString</tt>
     */
    implicit def GrizzledString_RichString(gs: GrizzledString) =
        new RichString(gs.string)

    /**
     * Implicit function to convert from a character to a <tt>GrizzledChar</tt>.
     *
     * @param s  the character
     *
     * @return the <tt>GrizzledChar</tt>
     */
    implicit def Char_GrizzledChar(c: Char) = new GrizzledChar(c)

    /**
     * Implicit function to convert from a <tt>GrizzledChar</tt> to a
     * character.
     *
     * @param gc  the <tt>GrizzledChar</tt>
     *
     * @return the character
     */
    implicit def GrizzledChar_Char(gc: GrizzledChar) = gc.character

    /**
     * Implicit function to convert from a Java <tt>Character</tt> object
     * to a <tt>GrizzledChar</tt>.
     *
     * @param s  the <tt>Character</tt> object
     *
     * @return the <tt>GrizzledChar</tt>
     */
    implicit def JavaCharacter_GrizzledChar(c: java.lang.Character) =
        new GrizzledChar(c.charValue)

    /**
     * Implicit function to convert from a <tt>GrizzledChar</tt> to a
     * Java <tt>Character</tt> object.
     *
     * @param gc  the <tt>GrizzledChar</tt>
     *
     * @return the <tt>Character</tt> object
     */
    implicit def GrizzledChar_JavaCharacter(c: GrizzledChar) =
        new java.lang.Character(c.character)


    /**
     * Implicit function to convert from a Scala <tt>RichChar</tt> object
     * to a <tt>GrizzledChar</tt>.
     *
     * @param s  the <tt>RichChar</tt> object
     *
     * @return the <tt>GrizzledChar</tt>
     */
    implicit def RichChar_GrizzledChar(c: RichChar) =
        new GrizzledChar(c.self.asInstanceOf[Char])

    /**
     * Implicit function to convert from a <tt>GrizzledChar</tt> to a
     * Scala <tt>RichChar</tt> object.
     *
     * @param gc  the <tt>GrizzledChar</tt>
     *
     * @return the <tt>RichChar</tt> object
     */
    implicit def GrizzledChar_RichChar(c: GrizzledChar) =
        new RichChar(c.character)
}

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

import scala.util.matching.Regex

/**
 * An analog to Scala's <tt>RichChar</tt> class, providing some methods
 * that neither <tt>RichChar</tt> nor <tt>Char</tt> (nor, for that matter,
 * <tt>java.lang.Character</tt>) provide. By importing the implicit
 * conversion functions, you can use the methods in this class
 * transparently from a <tt>Char</tt>, <tt>RichChar</tt> or
 * <tt>Character</tt> object.
 *
 * <blockquote><pre>
 * import grizzled.string.implicits._
 * val ch = 'a'
 * println(ch.isHexDigit) // prints: true
 * </pre></blockquote>
 */
final class GrizzledChar(val character: Char)
{
    /**
     * Determine whether the character represents a valid hexadecimal
     * digit. This is a specialization of <tt>isDigit(radix)</tt>.
     *
     * @return <tt>true</tt> if the character is a valid hexadecimal
     *         digit, <tt>false</tt> if not.
     */
    def isHexDigit = isDigit(16)

    /**
     * Determine whether the character represents a valid digit in a
     * given base.
     *
     * @param radix the radix
     *
     * @return <tt>true</tt> if the character is a valid digit in the
     *         indicated radix, <tt>false</tt> if not.
     */
    def isDigit(radix: Int): Boolean =
    {
        try
        {
            Integer.parseInt(character.toString, radix)
            true
        }
        catch
        {
            case _: NumberFormatException => false
        }
    }
}

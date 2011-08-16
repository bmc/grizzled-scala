/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2010-2011, Brian M. Clapper
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

/**
 * Some reflection-related utility methods and classes.
 */
object reflect
{
    import scala.reflect.Manifest

    /**
     * Determine whether an object is of a particular type. Example
     * of use:
     *
     * {{{
     * def foo(obj: Any) =
     * {
     *     // Is this object of type Seq[Int] or just Int?
     *     if (isOfType[Int](obj))
     *         ...
     *     else if (isOfType[Seq[Int]](obj))
     *         ...
     *     else
     *         ...
     * }
     * }}}
     *
     * @param  o  the object to test
     * @tparam T   the type to test against
     *
     * @return `true` if `o` is of type `T`, `false` if not.
     */
    def isOfType[T](o: Any)(implicit man: Manifest[T]): Boolean =
    {
        // The following is nice, but fails on "primitives" (e.g., Int).
        /*
        man >:> Manifest.classType(v.asInstanceOf[AnyRef].getClass)
        */
        def isPrimitive[P](implicit manP: Manifest[P]): Boolean =
            Class.forName(manP.toString).
                  isAssignableFrom(o.asInstanceOf[AnyRef].getClass)

        def isClass: Boolean =
            man.erasure.isAssignableFrom(o.asInstanceOf[AnyRef].getClass)

        man.toString match
        {
            case "Int"    => isPrimitive[java.lang.Integer]
            case "Short"  => isPrimitive[java.lang.Short]
            case "Long"   => isPrimitive[java.lang.Long]
            case "Float"  => isPrimitive[java.lang.Float]
            case "Double" => isPrimitive[java.lang.Double]
            case "Char"   => isPrimitive[java.lang.Character]
            case "Byte"   => isPrimitive[java.lang.Byte]
            case _        => isClass
        }
    }
}

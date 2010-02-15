/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2010, Brian M. Clapper
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
 * Miscellaneous utility functions and methods not otherwise categorized.
 */
object util
{
    /**
     * Used with any object that contains a <tt>close()</tt> method that
     * returns nothing, <tt>withCloseable()</tt> executes a block of code
     * with the closeable object, ensuring that the object is closed no
     * matter what. It allows you to replace code like this:
     *
     * <blockquote><pre>
     * val closeableObject = ...
     * try
     * {
     *     doSomethingWith(closeableObject)
     * }
     * finally
     * {
     *     closeableObject.close
     * }
     * </pre></blockquote>
     *
     * with:
     *
     * <blockquote><pre>
     * withCloseable(closeableObject)
     * {
     *     closeable =>
     *     doSomethingWith(closeable)
     * }
     * </pre></blockquote>
     *
     * @param thing   the closeable object
     * @param code    the block of code, which will take the closeable object
     *                and return some arbitrary type <tt>R</tt>.
     *
     * @return whatever the code block returns,if anything.
     */
    def withCloseable[T <: {def close(): Unit}, R](closeable: T)(code: T => R) =
    {
        try
        {
            code(closeable)
        }

        finally
        {
            closeable.close
        }
    }
}


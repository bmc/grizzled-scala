/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2009-2010 Brian M. Clapper. All rights reserved.

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

import org.scalatest.FunSuite
import grizzled.reflect._
import scala.reflect.Manifest
import scala.reflect.{ClassTag, classTag}
 
/**
  * Tests the grizzled.file functions.
  */
class ReflectionTest extends FunSuite {
  private def isOfTypeTest[T: ClassTag](expected: Boolean, v: Any): Unit = {
    assertResult(expected, "isOfType " + classTag.runtimeClass.toString) {
      isOfType[T](v) 
    }
  }

  test("isOfType primitives") {
    isOfTypeTest [Int] (true, 10)
    isOfTypeTest [Int] (false, 10L)

    isOfTypeTest [Long] (true, 10L)
    isOfTypeTest [Long] (false, 10)

    isOfTypeTest [Short] (true, 10.asInstanceOf[Short] )
    isOfTypeTest [Short] (false, 10)

    isOfTypeTest [Float] (true, 10.0f)
    isOfTypeTest [Float] (false, 10)
    isOfTypeTest [Float] (false, 10.0)

    isOfTypeTest [Double] (true, 10.0)
    isOfTypeTest [Double] (false, 10.0f)
    isOfTypeTest [Double] (false, 10)

    isOfTypeTest [Byte] (true, 127.asInstanceOf[Byte] )
    isOfTypeTest [Byte] (false, 127)
    isOfTypeTest [Byte] (false, 10L)
    isOfTypeTest [Byte] (false, 'c')

    isOfTypeTest [Char] (true, 'c')
    isOfTypeTest [Char] (false, 65)
    isOfTypeTest [Char] (false, 65.asInstanceOf[Byte])
  }

  test("isOfType non-primitives") {
    class Foo
    class Bar extends Foo

    isOfTypeTest [List[Char]] (true, List('a', 'b'))
    isOfTypeTest [Seq[Char]]  (true, List('a', 'b'))
    isOfTypeTest [Char]       (false, List('a', 'b'))
    isOfTypeTest [Foo]        (false, new Object)
    isOfTypeTest [Foo]        (true, new Foo)
    isOfTypeTest [Foo]        (true, new Bar)
    isOfTypeTest [Bar]        (false, new Foo)
  }
}

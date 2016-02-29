/*
  ---------------------------------------------------------------------------
  Copyright © 2009-2016, Brian M. Clapper. All rights reserved.

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

import org.scalatest.{FlatSpec, Matchers}
import grizzled.string.template._

/**
 * Tests the grizzled.string.StringTemplate functions.
 */
class StringTemplateSpec extends FlatSpec with Matchers {
  "UnixShellStringTemplate" should
    "substitute empty strings for bad vars in safe mode" in {

    val data = Map(

      ("$foo bar $bar ${baz} $",
       Map("foo" -> "FOO",
           "bar" -> "BARSKI",
           "baz" -> "YAWN")) -> Right("FOO bar BARSKI YAWN $"),

      ("$foo bar $bar ${baz} $_",
       Map("foo" -> "FOO",
           "bar" -> "BARSKI",
           "baz" -> "YAWN")) -> Right("FOO bar BARSKI YAWN "),

      ("$foo bar $bar ${baz} $frodo$",
       Map("foo" -> "FOO",
           "bar" -> "$foo",
           "baz" -> "YAWN")) -> Right("FOO bar FOO YAWN $"),

      ("""$foo bar $bar ${baz} \$""",
       Map("foo" -> "FOO",
           "bar" -> "BARSKI",
           "baz" -> "YAWN")) -> Right("FOO bar BARSKI YAWN $"),

      ("""$foo ${foobar?blitz}""",
       Map("foo" -> "FOO",
           "bar" -> "BARSKI",
           "baz" -> "YAWN")) -> Right("FOO blitz")
    )

    for {(input, expected) <- data
         (str, vars) = (input._1, input._2)} {
      val template = new UnixShellStringTemplate(vars.get, true)
      template.sub(str) shouldBe expected
    }
  }

  it should "abort on bad vars in unsafe mode" in {
    val data = Map(

      ("$foo bar $bar ${baz} $$ $x",
       Map("foo" -> "FOO",
           "bar" -> "BARSKI",
           "baz" -> "YAWN")) -> "FOO bar BARSKI YAWN $ ",

      ("$foo bar $bar ${baz} $_",
       Map("foo" -> "FOO",
           "bar" -> "BARSKI",
           "baz" -> "YAWN")) -> "FOO bar BARSKI YAWN ",

      ("$foo bar $bar ${baz} $frodo$",
       Map("foo" -> "FOO",
           "bar" -> "$foo",
           "baz" -> "YAWN")) -> "FOO FOO BARSKI YAWN $",

      ("$foo bar $bar ${baz} $y",
       Map("foo" -> "FOO",
           "bar" -> "BARSKI",
           "baz" -> "YAWN")) -> "FOO bar BARSKI YAWN $$"
    )

    for {(input, expected) <- data
         (str, vars) = (input._1, input._2)} {
      val template = new UnixShellStringTemplate(vars.get, false)
      template.sub(str).isLeft shouldBe true
    }
  }

  "WindowsCmdStringTemplate" should
    "substitute empty strings for bad vars in safe mode" in {

    val data = Map(

      ("%foo% bar %bar% %baz% %",
       Map("foo" -> "FOO",
           "bar" -> "BARSKI",
           "baz" -> "YAWN")) -> Right("FOO bar BARSKI YAWN %"),

      ("%foo% bar %bar% %baz% %_%",
       Map("foo" -> "FOO",
           "bar" -> "BARSKI",
           "baz" -> "YAWN")) -> Right("FOO bar BARSKI YAWN "),

      ("%foo% bar %bar% %baz% %frodo%x",
       Map("foo" -> "FOO",
           "bar" -> "%foo%",
           "baz" -> "YAWN")) -> Right("FOO bar FOO YAWN x"),

      ("%foo% bar %bar% %baz% %%",
       Map("foo" -> "FOO",
           "bar" -> "BARSKI",
           "baz" -> "YAWN")) -> Right("FOO bar BARSKI YAWN %")
    )

    for {(input, expected) <- data
         (str, vars) = (input._1, input._2)} {
      val template = new WindowsCmdStringTemplate(vars.get, true)
      template.sub(str) shouldBe expected
    }
  }

  it should "abort on bad vars in unsafe mode" in {
    val data = Map(

      ("%foo% bar %bar% ${baz} %% %x%",
       Map("foo" -> "FOO",
           "bar" -> "BARSKI",
           "baz" -> "YAWN")) -> "FOO bar BARSKI YAWN %",

      ("%foo% bar %bar% %baz% %_%",
       Map("foo" -> "FOO",
           "bar" -> "BARSKI",
           "baz" -> "YAWN")) -> "FOO bar BARSKI YAWN ",

      ("%foo% bar %bar% ${baz} %frodo%x",
       Map("foo" -> "FOO",
           "bar" -> "%foo%",
           "baz" -> "YAWN")) -> "FOO FOO BARSKI YAWN x",

      ("%foo% bar %bar% %baz% %y%",
       Map("foo" -> "FOO",
           "bar" -> "BARSKI",
           "baz" -> "YAWN")) -> "FOO bar BARSKI YAWN "
    )

    for {(input, expected) <- data
         (str, vars) = (input._1, input._2)} {
      val template = new WindowsCmdStringTemplate(vars.get, false)
      template.sub(str).isLeft shouldBe true
    }
  }
}



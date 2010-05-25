/*---------------------------------------------------------------------------*\
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2010 Brian M. Clapper. All rights reserved.

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
\*---------------------------------------------------------------------------*/

import org.scalatest.FunSuite
import grizzled.parsing.markup._

/**
 * Tests the grizzled.parsing.markup Textile functions.
 */
class TextileTest extends GrizzledFunSuite
{
    test("TextileParser.parseToHTML")
    {
        import scala.io.Source

        val data = List(
            ("*Test*",             "<p><strong>Test</strong></p>"),
            ("_Test_",             "<p><em>Test</em></p>"),
            ("**Test**",           "<p><b>Test</b></p>"),
            ("__Test__",           "<p><i>Test</i></p>"),
            ("-Test-",             "<p><del>Test</del></p>"),
            ("^Test^",             "<p><sup>Test</sup></p>"),
            ("~Test~",             "<p><sub>Test</sub></p>"),
            ("@code block@",       "<p><code>code block</code></p>"),
            ("h1. Test Header",    "<h1 id=\"TestHeader\">Test Header</h1>"),
            ("\"link\":#link",     "<p><a href=\"#link\">link</a></p>"),
            ("Trademark(tm)",      "<p>Trademark&#8482;</p>"),
            ("Registered(r)",      "<p>Registered&#174;</p>"),
            ("Copyright(c)",       "<p>Copyright&#169;</p>"),
            ("bc. foo",            "<pre><code>foo\n</code></pre>")
        )

        val parser = MarkupParser.getParser(MarkupType.Textile)
        for((input, expected) <- data)
        {
            expect(expected, "TextileParser.parseToHTML() on: " + input)
            {
                parser.parseToHTML(input)
            }
        }
    }
}

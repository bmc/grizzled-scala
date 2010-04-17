/*---------------------------------------------------------------------------*\
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
\*---------------------------------------------------------------------------*/

import org.scalatest.FunSuite
import grizzled.config._
import scala.io.Source

/**
 * Tests the grizzled.io functions.
 */
class ConfigTest extends GrizzledFunSuite
{
    test("basic configuration")
    {
        val configText = """
[section1]
var1 = val1
var2 = val2
[section2]
var1 = foo bar
"""

        val expected = Map(
            Some("val1")    -> ("retrieval", "section1", "var1"),
            Some("val2")    -> ("retrieval", "section1", "var2"),
            Some("foo bar") -> ("retrieval", "section2", "var1"),
            None            -> ("bad variable", "section2", "var2"),
            None            -> ("bad section", "section3", "anything")
        )

        doTest(configText, expected)
    }

    test("good variable substitution")
    {
        val configText = """
[vars]
foo = foobar

[section1]
home = ${env.HOME}
home2 = ${system.user.home}
foo = ${vars.foo}
"""
        val home = System.getProperty("user.home")

        val expected = Map(
            Some("foobar") -> ("retrieval", "vars", "foo"),
            Some(home)     -> ("substitution", "section1", "home"),
            Some(home)     -> ("substitution", "section1", "home2"),
            Some("foobar") -> ("substitution", "section1", "foo"),
            None           -> ("bad substitution", "section1", "bar")
        )

        doTest(configText, expected, safe=true)
    }

    test("bad variable substitution")
    {
        val data = List("[section1]\nfoobar = ${vars.foobar}",
                        "[vars]\nfoo = foobar\n[section1]\nbar: ${vars.bar}")

        for (configString <- data)
        {
            try
            {
                Configuration(Source.fromString(configString))
                fail("Did not get expected SubstitutionException")
            }

            catch
            {
                case _: SubstitutionException =>
            }
        }
    }

    test("legal getInt(), no default")
    {
        val configText = """
[section]
foo: 0
bar = 1
baz = 1001801
"""
        val data = Map("foo" -> Some(0),
                       "bar" -> Some(1),
                       "baz" -> Some(1001801))
        val config = Configuration(Source.fromString(configText))

        for ((opt, expected) <- data)
            expect(expected, opt + "=" + expected.toString)
            {
                config.getInt("section", opt)
            }

    }

    test("bad getInt(), no default")
    {
        val configText = """
[section]
foo: abc
bar: 100a
baz: asfdasdfasdf
"""
        val config = Configuration(Source.fromString(configText))

        for (opt <- config.optionNames("section"))
            try
            {
                config.getInt("section", opt)
                fail("Did not get expected ConversionException for " + opt)
            }
            catch
            {
                case _: ConversionException =>
            }

    }

    test("getIntOrElse(), taking default")
    {
        val configText = """
[section]
"""
        val data = Map("foo" -> 0,
                       "bar" -> 1,
                       "baz" -> 1001801)
        val config = Configuration(Source.fromString(configText))

        for ((opt, expected) <- data)
            expect(expected, opt + "=" + expected.toString)
            {
                config.getIntOrElse("section", opt, expected)
            }
    }

    test("getInt")
    {
        val configText = """
[section]
foo: 10
bar: 100
"""
        val data = Map("foo" -> Some(10),
                       "bar" -> Some(100),
                       "baz" -> None)
        val config = Configuration(Source.fromString(configText))

        for ((opt, expected) <- data)
            expect(expected, opt + " -> " + expected)
            {
                config.getInt("section", opt)
            }
    }

    test("legal getBoolean(), no default")
    {
        val configText = """
[section]
false0: 0
true1 = 1
falseno = no
trueyes = yes
falseoff = off
trueon = on
false = false
true = true
"""
        val config = Configuration(Source.fromString(configText))

        for (opt <- config.optionNames("section"))
        {
            val expected = if (opt.startsWith("false")) Some(false) 
                           else Some(true)
            expect(expected, opt + "=" + expected)
            {
                config.getBoolean("section", opt)
            }

        }
    }

    test("bad getBoolean(), no default")
    {
        val configText = """
[section]
foo: a
bar: treu
baz: felse
"""
        val config = Configuration(Source.fromString(configText))

        for (opt <- config.optionNames("section"))
            try
            {
                println(config.getBoolean("section", opt))
                fail("Did not get expected ConversionException for " + opt)
            }
            catch
            {
                case _: ConversionException =>
            }

    }

    test("getBooleanOrElse(), taking default")
    {
        val configText = """
[section]
"""
        val data = Map("foo" -> false,
                       "bar" -> true,
                       "baz" -> false)
        val config = Configuration(Source.fromString(configText))

        for ((opt, expected) <- data)
            expect(expected, opt + "=" + expected.toString)
            {
                config.getBooleanOrElse("section", opt, expected)
            }
    }

    test("getBoolean")
    {
        val configText = """
[section]
foo: true
bar: 0
"""
        val data = Map("foo" -> Some(true),
                       "bar" -> Some(false),
                       "baz" -> None)

        val config = Configuration(Source.fromString(configText))

        for ((opt, expected) <- data)
            expect(expected, opt + " -> " + expected)
            {
                config.getBoolean("section", opt)
            }
    }

    test("matchingSections")
    {
        val configText = """
[section]
foo: true
bar: 0
[section1]
foo: bar
[section2]
foo: bar
[foo]
foo: bar
[foobar]
foo: bar
[a1]
foo: bar
"""
        val data = Map(
            """^section""".r -> Set("section", "section1", "section2"),
            """^f""".r       -> Set("foo", "foobar"),
            """[0-9]$""".r   -> Set("a1", "section1", "section2"),
            """o""".r        -> Set("section", "section1", "section2", "foo",
                                    "foobar")
        )

        val config = Configuration(Source.fromString(configText))

        for ((regex, expectedMatches) <- data)
        {
            val matches = config.matchingSections(regex).map(_.name).toSet
            if (matches.size != expectedMatches.size)
            {
                fail("For \"" + regex.pattern + "\", expected " +
                     "matchingSections to return " + expectedMatches.size +
                     " matches, but got " + matches.size)
            }

            if ((matches intersect expectedMatches) != expectedMatches)
            {
                fail("For \"" + regex.pattern + "\", expected " +
                     "matchingSections to be " + expectedMatches +
                     ", but got " + matches)
            }
        }
    }

    test("getSection")
    {
        val configText = """
[section1]
foo: bar
[section2]
"""
        val data = Map("section1" -> true,
                       "section2" -> true,
                       "section3" -> false)

        val config = Configuration(Source.fromString(configText))
        for ((sectionName, wantSome) <- data)
        {
            config.getSection(sectionName) match
            {
                case Some(section) =>
                    if (! wantSome)
                        fail("Expected None for section \"" + sectionName +
                             "\", got: Some(" + section + ")")
                    if (section.name != sectionName)
                        fail("Got expected Some(Section), but wanted name \"" +
                             sectionName + "\", got \"" + section.name + "\"")

                case None =>
                    if (wantSome)
                        fail("Expected Some(Section) for section \"" +
                             sectionName + "\", got: None")
            }
        }
    }

    private def doTest(configString: String,
                       data: Map[Option[String],Tuple3[String,String,String]],
                       safe: Boolean = false) =
    {
        val config = Configuration(Source.fromString(configString), safe)

        for ((expectedResult, inputs) <- data)
        {
            val opIdent  = inputs._1
            val section  = inputs._2
            val variable = inputs._3

            expect(expectedResult, opIdent) {config.get(section, variable)}
        }
    }
}

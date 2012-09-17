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
import grizzled.file.util._

/**
 * Tests the grizzled.file functions.
 */
class FileTest extends FunSuite {
  test("basename") {
    val data = Map(("", "/")                 -> "",
                   ("foo", "/")              -> "foo",
                   ("foo/bar", "/")          -> "bar",
                   (".", "/")                -> ".",
                   ("../foo", "/")           -> "foo",
                   ("/foo/bar/baz", "/")     -> "baz",
                   ("////", "/")             -> "/",

                   ("", "\\")                -> "",
                   ("foo", "\\")             -> "foo",
                   ("foo\\bar", "\\")        -> "bar",
                   (".", "\\")               -> ".",
                   ("..\\foo", "\\")         -> "foo",
                   ("\\foo\\bar\\baz", "\\") -> "baz",
                   ("D:\\foo\\bar", "\\")    -> "bar")

    for(((path, sep), expected) <- data) {
      expect(expected, "basename(\"" + path + "\", \"" + sep + "\")")  { 
        basename(path, sep)
      }
    }
  }

  test("dirname") {
    val data = Map(("", "/")                  -> "",
                   ("foo", "/")               -> ".",
                   ("foo/bar", "/")           -> "foo",
                   (".", "/")                 -> ".",
                   ("../foo", "/")            -> "..",
                   ("/foo/bar/baz", "/")      -> "/foo/bar",
                   ("/foo", "/")             -> "/",
                   ("/foo", "/")             -> "/",
                   ("/", "/")                -> "/",
                   ("////", "/")             -> "/",

                   ("", "\\")                -> "",
                   ("foo", "\\")             -> ".",
                   ("foo\\bar", "\\")        -> "foo",
                   (".", "\\")               -> ".",
                   ("..\\foo", "\\")         -> "..",
                   ("\\foo\\bar\\baz", "\\") -> "\\foo\\bar",
                   ("\\foo", "\\")           -> "\\",
                   ("\\foo", "\\")           -> "\\",
                   ("\\", "\\")              -> "\\",
                   ("\\\\\\\\", "\\")        -> "\\")

    for(((path, sep), expected) <- data) {
      expect(expected, "dirname(\"" + path + "\", \"" + sep + "\")")  {
        dirname(path, sep)
      }
    }
  }

  test("dirnameBasename") {
    val data = Map(("", "/")                 -> ("", ""),
                   ("foo", "/")              -> (".", "foo"),
                   ("foo/bar", "/")          -> ("foo", "bar"),
                   (".", "/")                -> (".", ""),
                   ("../foo", "/")           -> ("..", "foo"),
                   ("./foo", "/")            -> (".", "foo"),
                   ("/foo/bar/baz", "/")     -> ("/foo/bar", "baz"),
                   ("/foo", "/")             -> ("/", "foo"),
                   ("/", "/")                -> ("/",  ""),

                   ("", "\\")                -> ("", ""),
                   ("foo", "\\")             -> (".", "foo"),
                   ("foo\\bar", "\\")        -> ("foo", "bar"),
                   (".", "\\")               -> (".", ""),
                   ("..\\foo", "\\")         -> ("..", "foo"),
                   (".\\foo", "\\")          -> (".", "foo"),
                   ("\\foo\\bar\\baz", "\\") -> ("\\foo\\bar", "baz"),
                   ("\\foo", "\\")           -> ("\\", "foo"),
                   ("\\", "\\")              -> ("\\",  ""),
                   ("D:\\foo\\bar", "\\")    -> ("D:\\foo", "bar"))

    for(((path, sep), expected) <- data) {
      expect(expected, "dirnameBasename(\"" + path + "\", \"" + sep + "\")")  {
        dirnameBasename(path, sep)
      }
    }
  }

  test("splitPath, Posix") {
    val data = Map(
      ("", "/")                  -> List[String](""),
      ("foo", "/")               -> List[String]("foo"),
      ("foo/bar", "/")           -> List[String]("foo", "bar"),
      (".", "/")                 -> List[String]("."),
      ("../foo", "/")            -> List[String]("..", "foo"),
      ("./foo", "/")             -> List[String](".", "foo"),
      ("/foo/bar/baz", "/")      -> List[String]("/foo", "bar", "baz"),
      ("foo/bar/baz", "/")       -> List[String]("foo", "bar", "baz"),
      ("/foo", "/")              -> List[String]("/foo"),
      ("/", "/")                 -> List[String]("/"),

      ("", "\\")                 -> List[String](""),
      ("foo", "\\")              -> List[String]("foo"),
      ("foo\\bar", "\\")         -> List[String]("foo", "bar"),
      (".", "\\")                -> List[String]("."),
      ("..\\foo", "\\")          -> List[String]("..", "foo"),
      (".\\foo", "\\")           -> List[String](".", "foo"),
      ("\\foo\\bar\\baz", "\\")  -> List[String]("\\foo", "bar", "baz"),
      ("foo\\bar\\baz", "\\")    -> List[String]("foo", "bar", "baz"),
      ("\\foo", "\\")            -> List[String]("\\foo"),
      ("\\", "\\")               -> List[String]("\\"),
      ("d:\\", "\\")             -> List[String]("d:\\")
    )
    
    for(((path, sep), expected) <- data) {
      expect(expected, "splitPath(\"" + path + "\", \"" + sep + "\")")  {
        splitPath(path, sep)
      }
    }
  }

  test("joinPath") {
    val data = Map(
      ("/", List(""))                       -> "",
      ("/", List("foo"))                    -> "foo",
      ("/", List("foo", "bar"))             -> "foo/bar",
      ("/", List("."))                      -> ".",
      ("/", List("foo", "bar", "baz"))      -> "foo/bar/baz",
      ("/", List("foo", "bar", "baz", ""))  -> "foo/bar/baz/",
      ("/", List("/foo", "bar", "baz"))     -> "/foo/bar/baz",
      ("/", List("/foo"))                   -> "/foo",
      ("/", List("/"))                      -> "/",

      ("\\", List(""))                      -> "",
      ("\\", List("foo"))                   -> "foo",
      ("\\", List("foo", "bar"))            -> "foo\\bar",
      ("\\", List("."))                     -> ".",
      ("\\", List("foo", "bar", "baz"))     -> "foo\\bar\\baz",
      ("\\", List("foo", "bar", "baz", "")) -> "foo\\bar\\baz\\",
      ("\\", List("\\foo", "bar", "baz"))   -> "\\foo\\bar\\baz",
      ("\\", List("\\foo"))                 -> "\\foo",
      ("\\", List("\\"))                    -> "\\",
      ("\\", List("d:\\"))                  -> "d:\\"
    )
    
    for(((sep, pieces), expected) <- data) {
      expect(expected, "joinPath(\"" + sep + "\"" + pieces) {
        joinPath(sep, pieces)
      }
    }
  }

  test("splitDrivePath") {
    val data = Map(
      ""                 -> ("", ""),
      ":"                -> ("", ""),
      "D:"               -> ("D:", ""),
      "c:\\"             -> ("c:", "\\"),
      "c:foo"            -> ("c:", "foo"),
      "c:foo\\bar"       -> ("c:", "foo\\bar"),
      "c:\\foo"          -> ("c:", "\\foo"),
      "\\foo"            -> ("", "\\foo"),
      "foo"              -> ("", "foo")
    )

    for((path, expected) <- data) {
      expect(expected, "splitDrivePath(\"" + path + "\"")  {
        splitDrivePath(path)
      }
    }
  }

  test("fnmatch") {
    val data = Map(("foo", "f*")          -> true,
                   ("foo", "f*o")         -> true,
                   ("foo", "f*b")         -> false,
                   ("foo", "*")           -> true,
                   ("foo", "*o")          -> true,
                   ("a.c", "*.c")         -> true,
                   ("abc", "[!a-r]*")     -> false,
                   ("radfa.c", "[!a-r]*") -> false,
                   ("radfa.c", "[^a-r]*") -> false,
                   ("sabc", "[!a-r]*")    -> true,
                   ("sabc", "[^a-r]*")    -> true)

    for(((string, pattern), expected) <- data) {
      expect(expected, 
             "fnmatch(\"" + string + "\", \"" + pattern + "\")") {
               fnmatch(string, pattern) 
             }
    }
  }

  test("normalizePosixPath") {
    val data = Map("/foo/../bar/////baz" -> "/bar/baz",
                   "///////foo/bar/" -> "/foo/bar",
                   "." -> ".",
                   "" -> ".",
                   "/" -> "/",
                   "//" -> "/",
                   "./." -> ".",
                   "./bar" -> "bar",
                   "///" -> "/",
                   "//////////////////." -> "/")

    for ((path, expected) <- data) {
      expect(expected, "normalizePosixPath(\"" + path + "\")")  {
        normalizePosixPath(path)
      }
    }
  }

  test("normalizeWindowsPath") {
    val data = Map(
      "\\" -> "\\",
      "c:\\foo\\" -> "c:\\foo",
      "c:\\foo" -> "c:\\foo",
      "c:\\foo\\bar" -> "c:\\foo\\bar",
      "\\\\server\\foo" -> "\\\\server\\foo",
      "\\\\server\\foo\\bar\\..\\baz" -> "\\\\server\\foo\\baz",
      "\\foo\\..\\bar\\\\\\\\\\baz" -> "\\bar\\baz",
      "c:\\foo\\bar\\..\\baz" -> "c:\\foo\\baz",
      "\\.." -> "\\",
      "\\..\\.." -> "\\"
    )

    for ((path, expected) <- data) {
      expect(expected, "normalizeWindowsPath(\"" + path + "\")")  {
        normalizeWindowsPath(path)
      }
    }
  }

  test("listRecursively") {
    import grizzled.file.GrizzledFile._
    import java.io.File

    withTemporaryDirectory("listRecursively") { f =>
      val paths = Set("foo/bar.c", "foo/baz.txt", "test.txt", "foo/bar/baz.txt")
      paths.map {new File(_)}.map {_.dirname}.foreach { f => f.mkdirs }
      println(f.listRecursively().toSet)
    }
  }
}

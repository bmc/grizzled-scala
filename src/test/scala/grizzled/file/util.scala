/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2009-2014 Brian M. Clapper. All rights reserved.

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

import org.scalatest.{FlatSpec, Matchers}
import grizzled.file.util._
import grizzled.file.GrizzledFile._
import java.io.File

import scala.util.Failure

/**
 * Tests the grizzled.file functions.
 */
class FileUtilTest extends FlatSpec with Matchers {
  "basename" should "handle all kinds of paths" in {
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
      basename(path, sep) shouldBe (expected)
    }
  }

  "dirname" should "handle all kinds of paths" in {
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
      dirname(path, sep) shouldBe (expected)
    }
  }

  "dirnameBasename" should "handle all kinds of files" in {
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
      dirnameBasename(path, sep) shouldBe (expected)
    }
  }

  "splitPath, Posix" should "handle all kinds of paths" in {
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
      splitPath(path, sep) shouldBe (expected)
    }
  }

  "joinPath" should "handle all kinds of paths" in {
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
      joinPath(sep, pieces) shouldBe (expected)
    }
  }

  "splitDrivePath" should "handle all kinds of paths" in {
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
      splitDrivePath(path) shouldBe (expected)
    }
  }

  "fnmatch" should "work" in {
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
      fnmatch(string, pattern) shouldBe (expected)
    }
  }

  "normalizePosixPath" should "work" in {
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
      normalizePosixPath(path) shouldBe (expected)
    }
  }

  "normalizeWindowsPath" should "work" in {
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
      normalizeWindowsPath(path) shouldBe (expected)
    }
  }

  "listRecursively" should "work" in {
    withTemporaryDirectory("list-recursively") { d =>
      val paths = Set("foo/bar.c", "foo/baz.txt", "test.txt", "foo/bar/baz.txt")
      val dirs =  paths.map { p => new File(joinPath(d.getPath, p)).dirname }

      dirs.foreach(_.mkdirs())
      val dirPaths: Set[String] = dirs.map(_.getPath)
      val expected: Set[String] = dirPaths + d.getPath

      listRecursively(d).length should be < paths.size
    }
  }

  "copy" should "fail if the directory doesn't exist" in {
    copy(Seq("foo.c"), "/nonexistent/directory", false).isFailure shouldBe (true)
  }

  it should "fail if the directory cannot be created" in {
    copy(Seq("foo.c"), "/etc/foo/bar/baz").isFailure shouldBe (true)
  }

  it should "fail if the source path doesn't exist" in {
    withTemporaryDirectory("copy") { d =>
      copy(Seq("foo.c"), d.getPath).isFailure shouldBe (true)
    }
  }

  it should "work if the source file and directory exist" in {
    withTemporaryDirectory("copy") { d =>
      import java.io._
      import grizzled.io.util._
      val sourceFile = File.createTempFile("foo", "txt")
      try {
        withCloseable(new FileWriter(sourceFile)) { f =>
          f.write("This is a test.\n")
        }
        val sourceSize = sourceFile.length
        copy(sourceFile.getPath, d.getPath).isSuccess shouldBe (true)
        val targetPath = joinPath(d.getPath, basename(sourceFile.getPath))
        new File(targetPath).length shouldBe (sourceSize)
      }

      finally {
        sourceFile.delete()
      }
    }
  }
}

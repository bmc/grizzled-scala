package grizzled.file

import java.util.regex.PatternSyntaxException

import grizzled.file.util._
import grizzled.util.CanReleaseResource.Implicits.CanReleaseAutoCloseable
import grizzled.util.withResource
import java.io.File
import java.nio.file.{Files, Path, Paths}

import grizzled.BaseSpec

import scala.util.Success

/**
 * Tests the grizzled.file functions.
 */
class FileUtilSpec extends BaseSpec {
  "basename" should "handle all kinds of paths" in {
    // The Map type annotations are mostly for IntelliJ, which (as of
    // 2.13.0-M5) gets confused and doesn't infer types properly, leading
    // to the dreaded squiggly reds.
    val data = Map[(String, String),String](
      ("", "/")                 -> "",
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
      ("D:\\foo\\bar", "\\")    -> "bar"
    )

    for(((path, sep), expected) <- data) {
      basename(path, sep) shouldBe expected
    }
  }

  "dirname" should "handle all kinds of paths" in {
    val data = Map[(String, String), String](
      ("", "/")                  -> "",
      ("foo", "/")               -> ".",
      ("foo/bar", "/")           -> "foo",
      (".", "/")                 -> ".",
      ("../foo", "/")            -> "..",
      ("/foo/bar/baz", "/")      -> "/foo/bar",
      ("/foo", "/")              -> "/",
      ("/foo", "/")              -> "/",
      ("/", "/")                 -> "/",
      ("////", "/")              -> "/",

      ("", "\\")                 -> "",
      ("foo", "\\")              -> ".",
      ("foo\\bar", "\\")         -> "foo",
      (".", "\\")                -> ".",
      ("..\\foo", "\\")          -> "..",
      ("\\foo\\bar\\baz", "\\")  -> "\\foo\\bar",
      ("\\foo", "\\")            -> "\\",
      ("\\foo", "\\")            -> "\\",
      ("\\", "\\")               -> "\\",
      ("\\\\\\\\", "\\")         -> "\\"
    )

    for(((path, sep), expected) <- data) {
      dirname(path, sep) shouldBe expected
    }
  }

  "dirnameBasename" should "handle all kinds of files" in {
    val data = Map[(String, String), (String, String)](
      ("", "/")                 -> ("", ""),
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
      ("D:\\foo\\bar", "\\")    -> ("D:\\foo", "bar")
    )

    for(((path, sep), expected) <- data) {
      dirnameBasename(path, sep) shouldBe expected
    }
  }

  "splitPath, Posix" should "handle all kinds of paths" in {
    val data = Map[(String, String), List[String]](
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
      splitPath(path, sep) shouldBe expected
    }
  }

  "joinPath" should "handle all kinds of paths" in {
    val data = Map[(String, List[String]), String](
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
      joinPath(sep, pieces) shouldBe expected
    }
  }

  "joinAndNormalizePath" should "normalize a joined path" in {
    val data = Seq[(List[String], String)](
      (List(".", "..", "foo", "bar"), "../foo/bar"),
      (List("..", ".", "foo", "..", "bar", "x"), "../bar/x")
    )

    for ((list, expected) <- data) {
      val expected2 = expected.replaceAll("/", File.separator)
      joinAndNormalizePath(list: _*) shouldBe expected2

      val list2 = list.map(new File(_))
      joinAndNormalizePath(list2: _*).getPath shouldBe expected2
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
      splitDrivePath(path) shouldBe expected
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
      fnmatch(string, pattern) shouldBe expected
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
      normalizePosixPath(path) shouldBe expected
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
      normalizeWindowsPath(path) shouldBe expected
    }
  }

  "listRecursively" should "work" in {
    import grizzled.file.Implicits._

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
    copy(Seq("foo.c"), "/nonexistent/directory", createTarget=false) shouldBe Symbol("failure")
  }

  it should "fail if the directory cannot be created" in {
    copy(Seq("foo.c"), "/etc/foo/bar/baz") shouldBe Symbol("failure")
  }

  it should "fail if the source path doesn't exist" in {
    withTemporaryDirectory("copy") { d =>
      copy(Seq("foo.c"), d.getPath) shouldBe Symbol("failure")
    }
  }

  it should "work if the source file and directory exist" in {
    withTemporaryDirectory("copy") { d =>
      import java.io._

      val sourceFile = File.createTempFile("foo", "txt")
      try {
        withResource(new FileWriter(sourceFile)) { f =>
          f.write("This is a test.\n")
        }
        val sourceSize = sourceFile.length
        copy(sourceFile.getPath, d.getPath) shouldBe Symbol("success")
        val targetPath = joinPath(d.getPath, basename(sourceFile.getPath))
        new File(targetPath).length shouldBe sourceSize
      }

      finally {
        sourceFile.delete()
      }
    }
  }

  "eglob" should """glob a "*" properly""" in {
    withTemporaryDirectory("glob") { d =>
      val paths = makeEmptyFiles(d.getAbsolutePath,
                                 Seq("aaa.txt", "bbb.txt", "ccc.java"))
      val expected = paths.filter(_.endsWith(".txt")).toSet

      val matches = eglob(joinPath(d.getPath, "*.txt")).toSet
      matches shouldBe expected
    }
  }

  it should "not match nonexistent files" in {
    withTemporaryDirectory("glob") { d =>
      val subdirs = makeDirectories(d.getAbsolutePath,
                                    Seq("one", "two", "three"))
      val subdirs2 = for { dir <- subdirs } yield {
        makeDirectories(dir, Seq("aaa", "bbb", "ccc"))
      }

      val file1 = makeEmptyFiles(d.getAbsolutePath, Seq("README")).head
      val file2 = makeEmptyFiles(subdirs.head, Seq("README")).head

      val matches = eglob(joinPath(d.getPath, "**", "README")).toSet
      matches shouldBe Set(file1, file2)
    }
  }

  it should "glob a character class properly" in {
    withTemporaryDirectory("glob") { d =>
      val paths = makeEmptyFiles(d.getAbsolutePath,
                                 Seq("abc.txt", "aaa.txt", "abd.txt",
                                       "abcdef.txt", "aaa.scala"))
      val matches = eglob(joinPath(d.getPath, "a[ab][a-z].txt")).toSet
      val expected = Set("aaa.txt", "abc.txt", "abd.txt")
      matches.map(basename(_)) shouldBe expected
    }
  }

  it should "glob a ? properly" in {
    withTemporaryDirectory("glob") { d =>
      val paths = makeEmptyFiles(d.getAbsolutePath,
                                 Seq("aba.txt", "aaa.txt", "abd.txt"))
      val matches = eglob(joinPath(d.getPath, "a?a.txt")).toSet
      matches.map(basename(_)) shouldBe Set("aaa.txt", "aba.txt")
    }
  }

  it should """handle "**" properly""" in {
    withTemporaryDirectory("glob") { d =>
      val fullDirPath = d.getAbsolutePath
      val subdirs = Seq("d1", "d2", "longer-dir-name")
      val files = Seq("a.scala", "foobar.scala", "README.md", "config.txt")
      val filePaths = subdirs.flatMap { subdir =>
        val subdirPath = joinPath(fullDirPath, subdir)
        new File(subdirPath).mkdirs()
        makeEmptyFiles(subdirPath, files)
      }

      val expected = filePaths.filter(_.endsWith(".scala")).toSet
      eglob(joinPath(fullDirPath, "**", "*.scala")).toSet shouldBe expected
    }
  }

  it should """ensure that a trailing "**" just gets directories""" in {
    withTemporaryDirectory("glob") { d =>
      val fullDirPath = d.getAbsolutePath
      val subdirs = Seq("d1", "d2", "longer-dir-name")
      val files = Seq("a.scala", "f.scala", "foobar.scala", "README.md",
                        "config.txt")
      val filePaths = subdirs.flatMap { subdir =>
        val subdirPath = joinPath(fullDirPath, subdir)
        new File(subdirPath).mkdirs()
        makeEmptyFiles(subdirPath, files)
      }

      val expected = subdirs.map(joinPath(fullDirPath, _)) :+ fullDirPath
      eglob(joinPath(fullDirPath, "**")).toSet shouldBe expected.toSet
    }
  }

  it should "handle an empty match" in {
    withTemporaryDirectory("glob") { d =>
      val globPath = joinPath(d.getAbsolutePath, "**", "*.scala")
      eglob(globPath).toSet shouldBe Set.empty[String]
    }
  }

  it should "bail on a bad glob pattern (though that isn't functional)" in {
    intercept[PatternSyntaxException] {
      eglob("[a-z")
    }
  }

  "glob" should "properly glob with a *" in {
    withTemporaryDirectory("glob") { d =>
      val fullDirPath = d.getAbsolutePath
      val simpleFilenames = Seq("foo.txt", "bar.c", "bar.txt")
      makeEmptyFiles(fullDirPath, simpleFilenames)
      val expected = simpleFilenames.filter(_ startsWith "bar")
                                    .map(s => joinPath(fullDirPath, s))
                                    .toSet
      glob(joinPath(fullDirPath, "bar*")).toSet shouldBe expected
    }
  }

  it should "properly glob with ?" in {
    withTemporaryDirectory("glob") { d =>
      val fullDirPath = d.getAbsolutePath
      val simpleFilenames = Seq("foo.txt", "bar.c", "boo.txt", "bar.txt")
      makeEmptyFiles(fullDirPath, simpleFilenames)
      val expected = simpleFilenames.filter(_ contains "oo.txt")
                                    .map(s => joinPath(fullDirPath, s))
                                    .toSet
      glob(joinPath(fullDirPath, "?oo.txt")).toSet shouldBe expected
    }
  }

  it should "properly glob with a character class" in {
    withTemporaryDirectory("glob") { d =>
      val fullDirPath = d.getAbsolutePath
      val simpleFilenames = Seq("foo.txt", "bar.c", "boo.txt", "bar.txt")
      makeEmptyFiles(fullDirPath, simpleFilenames)
      val expected = simpleFilenames.filter(_ contains "oo.txt")
                                    .map(s => joinPath(fullDirPath, s))
                                    .toSet
      glob(joinPath(fullDirPath, "[a-f]oo.txt")).toSet shouldBe expected
    }
  }

  it should "handle an empty match" in {
    withTemporaryDirectory("glob") { d =>
      val globPath = joinPath(d.getAbsolutePath, "*.scala")
      glob(globPath).toSet shouldBe Set.empty[String]
    }
  }

  it should "bail on a bad glob pattern (though that isn't functional)" in {
    intercept[PatternSyntaxException] {
      glob("[a-z")
    }
  }

  "longestCommonPathPrefix" should "work when only one component is common" in {
    longestCommonPathPrefix(List(
      "/foo/",
      "/foo/bar",
      "/foo/bar/baz"
    )) shouldBe "/foo/"

    longestCommonPathPrefix(List(
      "/foo",
      "/foo/bar",
      "/foo/bar/baz"
    )) shouldBe "/foo"
  }

  it should "return / on absolute files when no components are common" in {
    longestCommonPathPrefix(List(
      "/foo",
      "/bar",
      "/baz"
    )) shouldBe "/"
  }

  it should "work with relative paths that have a common prefix" in {
    longestCommonPathPrefix(List(
      "foo/baz",
      "foo/bar",
      "foo/bar/baz"
    )) shouldBe "foo/"
  }

  it should "return an empty string on relative paths with nothing in common" in {
    longestCommonPathPrefix(List(
      "foo/baz",
      "bar/baz",
      "foo/bar/baz"
    )) shouldBe ""
  }

  it should "work with one string, returning that string" in {
    longestCommonPathPrefix("/foo/bar/baz" :: Nil) shouldBe "/foo/bar/baz"
  }

  it should "work with an empty list, returning an empty string" in {
    longestCommonPathPrefix(Nil) shouldBe ""
  }

  "tryWithTemporaryDirectory" should "clean up & return Success on success" in {
    val t = tryWithTemporaryDirectory("foo") { dir: Path =>
      dir
    }

    t shouldBe Symbol("success")
    Files.exists(t.get) shouldBe false
  }

  it should "clean up and return Failure on failure" in {
    var savedDir: Path = Paths.get(".")
    val t = tryWithTemporaryDirectory("bar") { dir: Path =>
      savedDir = dir
      throw new Exception("intentional failure")
    }

    t shouldBe Symbol("failure")
    Files.exists(savedDir) shouldBe false
  }
}

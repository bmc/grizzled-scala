package grizzled.zip

import grizzled.util.withResource
import grizzled.file.{util => fileutil}
import fileutil.withTemporaryDirectory
import grizzled.file.Implicits.GrizzledFile
import grizzled.BaseSpec
import java.io._
import java.util.zip.ZipFile

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.io.Source
import scala.util.Random

class ZipperSpec extends BaseSpec {

  val fooContents =
    """|foo, line 1
       |
       |foo, line 3
       |""".stripMargin

  val barContents =
    """|File 2, line 1
       |
       |File 2, line 3
       |""".stripMargin

  var bazContents =
    """|Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
       |eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut
       |enim ad minim veniam, quis nostrud exercitation ullamco laboris
       |nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in
       |reprehenderit in voluptate velit esse cillum dolore eu fugiat
       |nulla pariatur. Excepteur sint occaecat cupidatat non proident,
       |sunt in culpa qui officia deserunt mollit anim id est laborum.
       |""".stripMargin

  "Zipper" should "create a flattened zip file that can be read back" in {
    val z = Zipper()

    withTemporaryDirectory("Zipper") { dir =>
      val absDir = dir.getAbsolutePath
      val f1 = fileutil.joinPath(absDir, "foo.txt")
      val f2 = fileutil.joinPath(absDir, "bar.txt")

      makeFiles(absDir, Array(
        ("foo.txt", fooContents),
        ("bar.txt", barContents)
      ))

      val t1 = z.addFile(f1, flatten = true)
      t1 shouldBe success

      val t2 = t1.get.addFile(f2, flatten = true)
      t2 shouldBe success

      val zipPath = new File(fileutil.joinPath(absDir, "out.zip"))
      t2.get.writeZip(zipPath) shouldBe success

      val zipFile = new ZipFile(zipPath)
      val entries = zipFile.entries.toSeq

      entries.exists(ze => ze.getName == "foo.txt") should be (true)
      readEntryAsChars(zipFile, "foo.txt") shouldBe fooContents

      entries.exists(ze => ze.getName == "bar.txt") should be (true)
      readEntryAsChars(zipFile, "bar.txt") shouldBe barContents
    }
  }

  it should "create a zip file with nested paths" in {
    val z = Zipper()

    withTemporaryDirectory("Zipper") { dir =>
      val absDir = dir.getAbsolutePath
      val f1Path = "foo/bar/foo.txt"
      val f2Path = "foo/bar/baz/quux.txt"
      val f1 = new File(fileutil.joinPath(absDir, f1Path))
      val f2 = new File(fileutil.joinPath(absDir, f2Path))

      f1.dirname.mkdirs
      withResource(new FileWriter(f1)) { w =>
        w.write(fooContents)
      }

      f2.dirname.mkdirs
      withResource(new FileWriter(f2)) { w =>
        w.write(barContents)
      }

      val t1 = z.addFile(f1, f1Path)
      t1 shouldBe success

      val t2 = t1.get.addFile(f2, f2Path)
      t2 shouldBe success

      val zipPath = new File(fileutil.joinPath(absDir, "out.zip"))
      t2.get.writeZip(zipPath) shouldBe success

      val zipFile = new ZipFile(zipPath)
      val entries = zipFile.entries.toSeq

      entries.exists(ze => ze.getName == "foo.txt") should be (false)
      entries.exists(ze => ze.getName == "bar.txt") should be (false)

      entries.exists(ze => ze.getName == f1Path) should be (true)
      readEntryAsChars(zipFile, f1Path) shouldBe fooContents

      entries.exists(ze => ze.getName == f2Path) should be (true)
      readEntryAsChars(zipFile, f2Path) shouldBe barContents
    }
  }

  it should "create a zip file with top-level files and files in directories" in {
    val z = Zipper()

    withTemporaryDirectory("Zipper") { dir =>
      val absDir = dir.getAbsolutePath
      val filesToCreate = Array(
        ("goof/hello.txt", fooContents),
        ("goof/ball/world.txt", barContents),
        ("baz.txt", bazContents),
        ("a/very/deeply/nested/directory/foo.txt", fooContents),
        ("a/very/shallow/baz.txt", bazContents)
      )

      makeFiles(absDir, filesToCreate)

      val files = filesToCreate.map { case (path, _) =>
        (new File(fileutil.joinPath(absDir, path)), path)
      }
      val t = Zipper(files)
      t shouldBe success
      val fullZipper = t.get


      val zipPath = new File(fileutil.joinPath(absDir, "out.zip"))
      fullZipper.writeZip(zipPath) shouldBe success

      val zipFile = new ZipFile(zipPath)
      val entries = zipFile.entries.toSeq

      for ((path, contents) <- filesToCreate) {
        entries.exists(_.getName == path) shouldBe true
        readEntryAsChars(zipFile, path) shouldBe contents
      }

      val dirs = filesToCreate.map(t => fileutil.dirname(t._1) + "/")
                        .filter(s => ! (s startsWith ".") )
      for (i <- dirs) {
        val e = entries.filter(_.getName == i).headOption
        e.map(_.getName) shouldBe Some(i)
        e.map(_.isDirectory) shouldBe Some(true)
      }
    }
  }

  it should "properly flatten paths" in {
    val z = Zipper()

    withTemporaryDirectory("Zipper") { dir =>
      val absDir = dir.getAbsolutePath
      val filesToCreate = Array(
        ("a/hello.txt", fooContents),
        ("a/subdir/world.txt", barContents),
        ("b/very/deeply/nested/directory/foo.txt", fooContents),
        ("b/very/shallow/baz.txt", bazContents)
      )

      makeFiles(absDir, filesToCreate)

      val files = filesToCreate.map { case (path, _) =>
        fileutil.joinPath(absDir, path)
      }
      val t = Zipper(files, flatten = true)
      t shouldBe success
      val fullZipper = t.get
      val zipPath = new File(fileutil.joinPath(absDir, "out.zip"))
      fullZipper.writeZip(zipPath) shouldBe success

      val zipFile = new ZipFile(zipPath)
      val entries = zipFile.entries.toSeq

      for ((path, contents) <- filesToCreate) {
        entries.exists(_.getName == path) shouldBe false
        val base = fileutil.basename(path)
        entries.exists(_.getName == base) shouldBe true
        readEntryAsChars(zipFile, base) shouldBe contents
      }
    }
  }

  it should "fail when flattening results in duplicates" in {
    val z = Zipper()

    withTemporaryDirectory("Zipper") { dir =>
      val absDir = dir.getAbsolutePath
      val filesToCreate = Array(
        ("a/hello.txt", fooContents),
        ("b/c/d/hello.txt", barContents)
      )

      val files = makeFiles(absDir, filesToCreate)
      val t = Zipper(files, flatten = true)

      t shouldBe failure
    }
  }

  it should "accept an entry from a URL" in {
    withTemporaryDirectory("Zipper") { dir =>
      val absDir = dir.getAbsolutePath
      val foo = createTextFile(dir, "foo.txt", fooContents)
      val bar = createTextFile(dir, "bar.txt", barContents)

      val z = Zipper()
      val t1 = z.addURL(foo.toURI.toURL, flatten = true)
      t1 shouldBe success
      val t2 = t1.get.addURL(bar.toURI.toURL, flatten = true)
      t2 shouldBe success
      val fullZipper = t2.get

      val zipPath = new File(fileutil.joinPath(absDir, "outurl.zip"))
      fullZipper.writeZip(zipPath) shouldBe success

      val zipFile = new ZipFile(zipPath)
      readEntryAsChars(zipFile, "foo.txt") shouldBe fooContents
      readEntryAsChars(zipFile, "bar.txt") shouldBe barContents
    }
  }

  it should "accept a binary entry from an InputStream" in {
    withTemporaryDirectory("Zipper") { dir =>
      val absDir = dir.getAbsolutePath
      val buf = randomByteArray(1024 * 1024)

      val z = Zipper()
      val entryName = "foobar/bytes.dat"
      val t = z.addInputStream(new ByteArrayInputStream(buf), entryName)
      t shouldBe success
      val zipPath = new File(fileutil.joinPath(absDir, "bin.zip"))
      t.get.writeZip(zipPath) shouldBe success

      val zipFile = new ZipFile(zipPath)
      val bytes = readEntryAsBytes(zipFile, entryName)
      bytes should have length buf.length
      bytes shouldBe buf
    }
  }

  it should "accept a binary entry from a byte array" in {
    withTemporaryDirectory("Zipper") { dir =>
      val absDir = dir.getAbsolutePath
      val buf = randomByteArray(1024 * 1024)

      val z = Zipper()
      val entryName = "foo/bar/bytes.dat"
      val t = z.addBytes(buf, entryName)
      t shouldBe success
      val zipPath = new File(fileutil.joinPath(absDir, "binary.zip"))
      t.get.writeZip(zipPath) shouldBe success

      val zipFile = new ZipFile(zipPath)
      val bytes = readEntryAsBytes(zipFile, entryName)
      bytes should have length buf.length
      bytes shouldBe buf
    }
  }

  it should "accept an entry from a Reader" in {
    withTemporaryDirectory("Zipper") { dir =>
      val absDir = dir.getAbsolutePath
      val reader = new StringReader(fooContents)
      val z = Zipper()
      val entryName = "baz/bar/foo.txt"
      val t = z.addReader(reader, entryName)
      t shouldBe success
      val t2 = t.get.addReader(new StringReader(bazContents), "baz.txt")
      t2 shouldBe success

      val zipPath = new File(fileutil.joinPath(absDir, "bin.zip"))
      t2.get.writeZip(zipPath) shouldBe success

      val zipFile = new ZipFile(zipPath)
      readEntryAsChars(zipFile, entryName) shouldBe fooContents
      readEntryAsChars(zipFile, "baz.txt") shouldBe bazContents
    }
  }

  it should "write a jar file with a manifest" in {
    import java.util.jar.{JarFile, Manifest => JarManifest}
    val manifestData =
      """|Manifest-Version: 1.0
         |Implementation-Title: grizzled-scala
         |Specification-Vendor: org.clapper
         |Specification-Title: grizzled-scala
         |Implementation-URL: http://software.clapper.org/grizzled-scala/
         |Implementation-Vendor: org.clapper
         |""".stripMargin

    val buf = new ByteArrayOutputStream()
    val writer = new OutputStreamWriter(buf)
    writer.write(manifestData)
    writer.close()
    val manifest = new JarManifest(new ByteArrayInputStream(buf.toByteArray))

    withTemporaryDirectory("Zipper") { dir =>
      val absDir = dir.getAbsolutePath
      val binaryFile = randomByteArray(1024 * 2048)
      val z = Zipper()
      val entryName = "grizzled/NotValidClass.class"

      val t = z.addInputStream(new ByteArrayInputStream(binaryFile), entryName)
      t shouldBe success

      val jarPath = new File(fileutil.joinPath(dir.getAbsolutePath, "foo.jar"))
      t.get.writeJar(jarPath, Some(manifest))

      val jarFile = new JarFile(jarPath)
      val manifest2 = Option(jarFile.getManifest)
      manifest2 shouldBe Some(manifest) // manifests should match
      readEntryAsBytes(jarFile, entryName) shouldBe binaryFile
    }
  }

  it should "allow adding an explicit zip directory entry" in {
    val z = Zipper()
    val zip = fileutil.joinPath(System.getProperty("java.io.tmpdir"), "out.zip")
    val t = z.addZipDirectory("foo/")
    t shouldBe success
    t.get.writeZip(zip)
    val zipFile = new ZipFile(zip)

    val entryOpt = zipFile.entries.toSeq.find(_.getName == "foo/")
    entryOpt.map(_.getName) shouldBe Some("foo/")
    entryOpt.map(_.isDirectory) shouldBe Some(true)
  }

  it should "fail to add an zip directory entry if it doesn't end in /" in {
    val z = Zipper()
    z.addZipDirectory("foo") shouldBe failure
  }

  it should "allow adding an explicit zip directory that's also implied" in {
    withTemporaryDirectory("Zipper") { dir =>
      val absDir = dir.getAbsolutePath
      val z = Zipper()
      val zip = fileutil.joinPath(absDir, "out.zip")
      val t1 = z.addZipDirectory("foo/")
      t1 shouldBe success

      val t2 = t1.get.addFile(new File("build.sbt"), "foo/bar/build.sbt")
      t2 shouldBe success

      t2.get.writeZip(zip) shouldBe success

      val zipFile = new ZipFile(zip)
      val matches = zipFile.entries.toSeq.filter(_.getName == "foo/")
      matches should have length 1

      val entry = matches.head
      entry.getName shouldBe "foo/"
      entry.isDirectory shouldBe true
    }
  }

  it should "allow retrieving the list of paths to be written" in {
    val z = Zipper()
    val t = z.addZipDirectory("foo/")
    t shouldBe success
    val t2 = t.get.addFile("build.sbt")
    t2 shouldBe success
    val t3 = t2.get.addFile("project/build.properties")
    t3 shouldBe success
    t3.get.paths shouldBe Set("foo/", "build.sbt", "project/build.properties")
  }

  it should " recursively add all files in a directory, stripped" in {
    testAddDirectory { (dir: File, files: Array[(String, String)]) =>

      val dirPath = dir.getPath
      val z = Zipper()
      val t = z.addDirectory(dir   = new File(fileutil.joinPath(dirPath, "src")),
                             strip = Some(dirPath))
      t shouldBe success
      val z2 = t.get
      val paths = z2.paths
      for (path <- files.map(_._1)) {
        paths should contain (path)
      }
    }
  }

  it should "recursively add all files in a directory, not stripped" in {
    testAddDirectory { (dir: File, files: Array[(String, String)]) =>

      val dirPath = dir.getPath
      val z = Zipper()
      val t = z.addDirectory(new File(fileutil.joinPath(dirPath, "src")))
      t shouldBe success
      val z2 = t.get
      val paths = z2.paths
      val prefix = dirPath.tail
      for (path <- files.map(_._1)) {
        paths should contain (prefix + "/" + path)
      }
    }
  }

  it should "recursively add all files in a directory, flattened" in {
    testAddDirectory { (dir: File, files: Array[(String, String)]) =>

      val dirPath = dir.getPath
      val z = Zipper()
      val t = z.addDirectory(
        dir     = new File(fileutil.joinPath(dirPath, "src")),
        flatten = true
      )
      t shouldBe success
      val z2 = t.get
      val paths = z2.paths
      val prefix = dirPath.tail
      for (path <- files.map(_._1)) {
        paths should contain (fileutil.basename(path))
      }
    }
  }

  it should "recursively add all files in a directory that match a pattern" in {
    testAddDirectory { (dir: File, files: Array[(String, String)]) =>

      val dirPath = dir.getPath
      val z = Zipper()
      val pattern = "*.txt"
      val t = z.addDirectory(
        dir      = new File(fileutil.joinPath(dirPath, "src")),
        wildcard = Some(pattern)
      )
      t shouldBe success
      val z2 = t.get
      val paths = z2.paths
      val prefix = dirPath.tail
      val fileNames = files.map(_._1)
      for (path <- fileNames.filter(fileutil.fnmatch(_, pattern)))
        paths should contain (prefix + "/" + path)
      for (path <- fileNames.filter(! fileutil.fnmatch(_, pattern))) {
        paths should not contain (prefix + "/" + path)
      }
    }
  }

  it should "have an empty comment by default" in {
    withTemporaryDirectory("Zipper") { dir =>
      val absDir = dir.getAbsolutePath
      val f = makeFile(absDir, "foobar.txt", "Some text\n")
      val z = Zipper()
      val t = z.addFile(f, flatten = true)
      t shouldBe success
      val zipFile = new File(fileutil.joinPath(absDir, "foo.zip"))
      t.get.writeZip(zipFile) shouldBe success
      val zip = new ZipFile(zipFile)
      Option(zip.getComment) shouldBe None
    }
  }

  it should "permit adding a comment" in {
    withTemporaryDirectory("Zipper") { dir =>
      val absDir = dir.getAbsolutePath
      val f = makeFile(absDir, "foobar.txt", "Some text\n")
      val z = Zipper()
      val t = z.addFile(f, flatten = true)
      t shouldBe success
      val zipFile = new File(fileutil.joinPath(absDir, "foo.zip"))
      val z2 = t.get
      val comment = "Written by ZipperSpec"
      z2.setComment(comment).writeZip(zipFile) shouldBe success
      val zip = new ZipFile(zipFile)
      Option(zip.getComment) shouldBe Some(comment)
    }
  }

  def testAddDirectory(code: (File, Array[(String, String)]) => Unit): Unit = {
    withTemporaryDirectory("Zipper") { dir =>
      val absDir = dir.getAbsoluteFile
      val filesToCreate = Array(
        ("src/goof/hello.md", fooContents),
        ("src/goof/ball/world", barContents),
        ("src/bar.txt", barContents),
        ("src/a/very/deeply/nested/directory/foo.txt", fooContents),
        ("src/a/very/shallow/baz.txt", bazContents)
      )

      makeFiles(absDir.getPath, filesToCreate)

      code(absDir, filesToCreate)
    }
  }

  def randomByteArray(size: Int) = {
    val buf = new Array[Byte](size)
    Random.nextBytes(buf)
    buf
  }

  def makeFiles(directory:        String,
                pathsAndContents: Array[(String, String)]): Array[File] = {
    for ((path, contents) <- pathsAndContents) yield {
      makeFile(directory, path, contents)
    }
  }

  def makeFile(directory: String, path: String, contents: String): File = {
    val fullPath = fileutil.joinPath(directory, path)
    val f = new File(fullPath)
    f.dirname.mkdirs
    withResource(new FileWriter(f)) { _.write(contents) }
    f
  }

  def readEntryAsChars(zipFile: ZipFile, entryName: String): String = {
    val e = zipFile.entries.toSeq.filter(_.getName == entryName).head
    Source.fromInputStream(zipFile.getInputStream(e), "UTF-8").mkString
  }

  def readEntryAsBytes(zipFile: ZipFile, entryName: String): Array[Byte] = {
    val e = zipFile.entries.toSeq.filter(_.getName == entryName).head
    val buf = new Array[Byte](e.getSize.toInt)
    withResource(zipFile.getInputStream(e)) { is =>
      @tailrec
      def readNext(offset: Int, total: Int): Int = {
        is.read(buf, offset, buf.length - offset) match {
          case n if n <= 0 => total
          case n => readNext(offset + n, total + n)
        }
      }

      readNext(0, 0)
    }
    buf
  }
}

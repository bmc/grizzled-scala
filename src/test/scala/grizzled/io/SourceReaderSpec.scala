package grizzled.io

import java.io.{FileWriter, IOException}

import grizzled.BaseSpec
import grizzled.file.util.{joinPath, withTemporaryDirectory}
import grizzled.util.withResource
import grizzled.util.CanReleaseResource.Implicits.CanReleaseAutoCloseable

import scala.io.Source
import scala.util.Random

class SourceReaderSpec extends BaseSpec {
  "read()" should "behave like Reader.read()" in {
    val sr = SourceReader(Source.fromString("abc"))
    sr.read() shouldBe 'a'
    sr.read() shouldBe 'b'
    sr.read() shouldBe 'c'
    sr.read() shouldBe -1
    sr.read() shouldBe -1
  }

  it should "fill a buffer, if the buffer is smaller than the remaining data" in {
    val s = (1 to 64).map { _ => Random.nextPrintableChar() }.mkString
    val sr = SourceReader(Source.fromString(s))
    val buf = new Array[Char](s.length / 2)
    sr.read(buf, 0, buf.length) shouldBe buf.length
    buf.mkString shouldBe s.take(s.length / 2)
  }

  it should "partially fill a buffer, if the buffer is too large" in {
    val s = (1 to 64).map { _ => Random.nextPrintableChar() }.mkString
    val sr = new SourceReader(Source.fromString(s))
    val buf = new Array[Char](s.length * 2)
    val n = sr.read(buf, 0, buf.length)
    n shouldBe s.length
    buf.take(n).mkString shouldBe s
  }

  it should "fill at an offset in the buffer" in {
    val s = "0123456789"
    val bufContents = "abcdefghijklmnopqrstuvwxyz"
    val buf: Array[Char] = bufContents.toArray
    val sr = SourceReader(Source.fromString(s))
    val offset = 10
    val total = 3
    val n = sr.read(buf, offset, total)
    val expected = bufContents.take(offset) +
                   s.take(total) +
                   bufContents.drop(offset + total)
    buf.mkString shouldBe expected
  }

  "skip()" should "skip 0 characters" in {
    val sr = SourceReader(Source.fromString("abc"))
    sr.skip(0) shouldBe 0
    sr.read() shouldBe 'a'
  }

  it should "skip only as many characters as there are in the Source" in {
    val sr = SourceReader(Source.fromString("abc"))
    sr.skip(4) shouldBe 3
    sr.read() shouldBe -1
  }

  it should "skip the right number of characters" in {
    val sr = SourceReader(Source.fromString("abcdefghijklmnopqrstuvwxyz"))
    sr.skip(10) shouldBe 10
    sr.read() shouldBe 'k'
  }

  "reset()" should "work on a string Source" in {
    val s = "abcdefghijklmnopqrstuvwxyz"
    val sr = new SourceReader(Source.fromString(s))
    val buf = new Array[Char](s.length)
    sr.read(buf, 0, buf.length) shouldBe buf.length
    sr.reset()
    sr.read(buf, 0, buf.length) shouldBe buf.length
  }

  it should "work on a file Source" in {
    withTemporaryDirectory("SourceReader") { dir =>
      val absDir = dir.getAbsolutePath
      val file = joinPath(absDir, "foo.txt")
      val s = "abcdefghijklmnopqrstuvwxyz"
      withResource(new FileWriter(file)) { w =>
        w.write(s)
      }
      val sr = SourceReader(Source.fromFile(file))
      val buf = new Array[Char](s.length)
      sr.read(buf, 0, buf.length) shouldBe buf.length
      sr.reset()
      sr.read(buf, 0, buf.length) shouldBe buf.length
    }
  }

  "mark()" should "throw an unconditional IOException" in {
    val s = "abcdefghijklmnopqrstuvwxyz"
    val sr = SourceReader(Source.fromString(s))
    an [IOException] should be thrownBy { sr.mark(10) }
  }

  "markSupported()" should "unconditionally return false" in {
    SourceReader(Source.fromFile("build.sbt")).markSupported shouldBe false
    SourceReader(Source.fromString("abc")).markSupported shouldBe false
  }

  "close()" should "close the underlying Source" in {
    withTemporaryDirectory("SourceReader") { dir =>
      val absDir = dir.getAbsolutePath
      val file = joinPath(absDir, "foo.txt")
      val s = "abcdefghijklmnopqrstuvwxyz"
      withResource(new FileWriter(file)) { w =>
        w.write(s)
      }

      val src = SourceReader(Source.fromFile(file))
      src.close()
      an [IOException] should be thrownBy { src.read() }
    }
  }
}

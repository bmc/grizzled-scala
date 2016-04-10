package grizzled.file

import java.io.{FileWriter, PrintWriter, File}

import org.scalatest.{FlatSpec, Matchers}
import grizzled.file.{util => FileUtil}
import FileUtil.withTemporaryDirectory
import grizzled.util.withResource

import scala.io.Source

/** Created by bmc on 4/10/16
  *
  */
class IncluderSpec extends FlatSpec with Matchers {
  private def createFile(dir: File, file: String, lines: String): File = {
    val path = FileUtil.joinPath(dir.getPath, file)
    withResource(new PrintWriter(new FileWriter(path))) { w =>
      lines.split("\n").foreach(w.println)
    }
    new File(path)
  }

  "Includer" should "handle a file including another file" in {
    withTemporaryDirectory("incl") { dir =>
      val input = createFile(dir, "outer.txt",
        """|This is a normal line.
           |%include "inner.txt"
           |This is another normal line.""".stripMargin)
      createFile(dir, "inner.txt",
        """|Inner file line 1.
           |Inner file line 2.""".stripMargin)
      Includer(Source.fromFile(input)).mkString("\n") shouldBe
        """|This is a normal line.
           |Inner file line 1.
           |Inner file line 2.
           |This is another normal line.""".stripMargin
    }
  }

  it should "handle a file including another file including another file" in {
    withTemporaryDirectory("incl") { dir =>
      val input = createFile(dir, "main.txt",
        """|main line 1
           |%include "inner1.txt"
           |main line 3""".stripMargin)
      createFile(dir, "inner1.txt",
        """|inner 1.1
           |%include "inner2.txt"
           |inner 1.3""".stripMargin)
      createFile(dir, "inner2.txt",
        """|inner 2.1
           |inner 2.2""".stripMargin)
      Includer(Source.fromFile(input)).mkString("\n") shouldBe
        """|main line 1
           |inner 1.1
           |inner 2.1
           |inner 2.2
           |inner 1.3
           |main line 3""".stripMargin
    }
  }

  it should "include from a Source that's a string, not a file" in {

  }


}

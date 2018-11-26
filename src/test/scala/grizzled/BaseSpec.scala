package grizzled

import java.io.{File, FileWriter}

import org.scalatest.{FlatSpec, Matchers}

import scala.sys.SystemProperties

/** Base spec for tests.
  */
class BaseSpec extends FlatSpec with Matchers {
  import grizzled.file.util.joinPath
  import grizzled.util.withResource
  import grizzled.util.CanReleaseResource.Implicits.CanReleaseAutoCloseable

  val lineSep = (new SystemProperties).getOrElse("line.separator", "\n")

  def createTextFile(dir: File, filename: String, contents: String): File = {
    val file = new File(joinPath(dir.getAbsolutePath, filename))
    withResource(new FileWriter(file)) { _.write(contents) }
    file
  }

  def createTextFile(dir: File, filename: String, contents: Array[String]): File = {
    createTextFile(dir, filename, contents.mkString(lineSep))
  }

  def makeEmptyFiles(directory: String, files: Seq[String]): Seq[String] = {
    for (fname <- files) yield {
      val path = joinPath(directory, fname)
      new File(path).createNewFile()
      path
    }
  }

  def makeDirectories(directory: String, subdirs: Seq[String]): Seq[String] = {
    for (dname <- subdirs) yield {
      val path = joinPath(directory, dname)
      new File(path).mkdirs()
      path
    }
  }
}

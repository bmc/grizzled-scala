package grizzled.net

import java.io.{File, FileWriter}
import java.net.{URL => JavaURL}

import grizzled.file.{util => fileutil}
import fileutil.withTemporaryDirectory
import grizzled.BaseSpec
import grizzled.util.withResource

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.Source

class URLUtilSpec extends BaseSpec {

  val WebURL = "https://raw.githubusercontent.com/bmc/grizzled-scala/master/README.md"

  val Contents =
    """|Lorem ipsum dolor sit amet, consectetur adipiscing
       |elit, sed do eiusmod tempor incididunt ut labore et
       |dolore magna aliqua. Ut enim ad minim veniam, quis
       |nostrud exercitation ullamco laboris nisi ut aliquip ex
       |ea commodo consequat. Duis aute irure dolor in
       |reprehenderit in voluptate velit esse cillum dolore eu
       |fugiat nulla pariatur. Excepteur sint occaecat
       |cupidatat non proident, sunt in culpa qui officia
       |deserunt mollit anim id est laborum."
       |
       |Sed ut perspiciatis unde omnis iste natus error sit
       |voluptatem accusantium doloremque laudantium, totam rem
       |aperiam, eaque ipsa quae ab illo inventore veritatis et
       |quasi architecto beatae vitae dicta sunt explicabo.
       |Nemo enim ipsam voluptatem quia voluptas sit aspernatur
       |aut odit aut fugit, sed quia consequuntur magni dolores
       |eos qui ratione voluptatem sequi nesciunt. Neque porro
       |quisquam est, qui dolorem ipsum quia dolor sit amet,
       |consectetur, adipisci velit, sed quia non numquam eius
       |modi tempora incidunt ut labore et dolore magnam
       |aliquam quaerat voluptatem. Ut enim ad minima veniam,
       |quis nostrum exercitationem ullam corporis suscipit
       |laboriosam, nisi ut aliquid ex ea commodi consequatur?
       |Quis autem vel eum iure reprehenderit qui in ea
       |voluptate velit esse quam nihil molestiae consequatur,
       |vel illum qui dolorem eum fugiat quo voluptas nulla
       |pariatur?
       |""".stripMargin


  def urlForContents(dir: File, name: String): JavaURL = {
    createTextFile(dir, name, Contents).toURI.toURL
  }

  "download" should "download from a file URL object" in {
    withTemporaryDirectory("URLUtil") { dir =>
      val url = URL(urlForContents(dir, "foo.txt"))
      val fut = URLUtil.download(url)
      val result = Await.result(fut, 10.seconds)
      Source.fromFile(result).mkString shouldBe Contents
    }
  }

  it should "download from a web server" in {
    withTemporaryDirectory("URLUtil") { dir =>
      val url = URL(WebURL).get
      val fut = URLUtil.download(url)
      val result = Await.result(fut, 10.seconds)
      val contents = Source.fromFile(result).mkString
      contents.length should be > 0
    }
  }

  it should "download from a string URL" in {
    withTemporaryDirectory("URLUtil") { dir =>
      val urlString = urlForContents(dir, "bar.txt").toExternalForm
      val fut = URLUtil.download(urlString)
      val result = Await.result(fut, 10.seconds)
      Source.fromFile(result).mkString shouldBe Contents
    }
  }

  it should "download to a file of my choosing" in {
    withTemporaryDirectory("download") { dir =>
      val url = urlForContents(dir, "foobar.txt")
      val file = fileutil.joinPath(dir.getAbsolutePath, "lorem.txt")
      val fut = URLUtil.download(url, file)
      Await.result(fut, 10.seconds)
      Source.fromFile(file).mkString shouldBe Contents
    }
  }

  "withDownloadedFile" should "download synchronously from a file" in {
    import URLUtil._
    withTemporaryDirectory("download") { dir =>
      val url = urlForContents(dir, "foobar.txt")
      val t = withDownloadedFile(url, 10.seconds) { f =>
        f.exists shouldBe true
        Source.fromFile(f).mkString
      }

      t shouldBe success
      t.get shouldBe Contents
    }
  }

  "withDownloadedFile" should "download synchronously from a web server" in {
    import URLUtil._
    val url = URL(WebURL).get
    val t = withDownloadedFile(url, 10.seconds) { f =>
      f.exists shouldBe true
      Source.fromFile(f).mkString
    }

    t shouldBe success
    t.get.length should be > 0
  }
}

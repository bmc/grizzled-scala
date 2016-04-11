package grizzled.net

import org.scalatest.{FlatSpec, Matchers}

import grizzled.testutil.BrainDeadHTTP._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.Source

class URLUtilSpec extends FlatSpec with Matchers {

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

  val DownloadHandler = Handler("thing.txt", { _ =>
    Response(ResponseCode.OK, Some(Contents))
  })

  val DownloadURL = "http://localhost:@PORT@/thing.txt"

  "download" should "download from a URL object" in {
    withHTTPServer(new Server(DownloadHandler)) { server =>
      val url = URL(DownloadURL.replace("@PORT@", server.bindPort.toString)).get
      val fut = URLUtil.download(url)
      val result = Await.result(fut, 10.seconds)
      Source.fromFile(result).mkString shouldBe Contents
    }
  }

  it should "download from a string URL" in {
    withHTTPServer(new Server(DownloadHandler)) { server =>
      val url = DownloadURL.replace("@PORT@", server.bindPort.toString)
      val fut = URLUtil.download(url)
      val result = Await.result(fut, 10.seconds)
      Source.fromFile(result).mkString shouldBe Contents
    }
  }

  it should "download to a file of my choosing" in {
    import grizzled.file.{util => FileUtil}
    import FileUtil.withTemporaryDirectory

    withTemporaryDirectory("download") { dir =>
      val file = FileUtil.joinPath(dir.getPath, "lorem.txt")
      withHTTPServer(new Server(DownloadHandler)) { server =>
        val url = DownloadURL.replace("@PORT@", server.bindPort.toString)
        val fut = URLUtil.download(url, file)
        Await.result(fut, 10.seconds)
        Source.fromFile(file).mkString shouldBe Contents
      }
    }
  }

  "withDownloadedFile" should "download synchronously" in {
    import URLUtil._

    withHTTPServer(new Server(DownloadHandler)) { server =>
      val url = DownloadURL.replace("@PORT@", server.bindPort.toString)
      val t = withDownloadedFile(url, 10.seconds) { f =>
        f.exists shouldBe true
        Source.fromFile(f).mkString
      }

      t.isSuccess shouldBe true
      t.get shouldBe Contents
    }
  }

  it should "timeout if the server takes too long" in {
    import URLUtil._

    val handler = DownloadHandler.copy(handle = { _ =>
      Thread.sleep(3000)
      Response(ResponseCode.OK, Some(Contents))
    })

    withHTTPServer(new Server(handler)) { server =>
      val url = DownloadURL.replace("@PORT@", server.bindPort.toString)
      val t = withDownloadedFile(url, 50.milliseconds) { f =>
        Source.fromFile(f).mkString
      }

      intercept[java.util.concurrent.TimeoutException] {
        t.get
      }
    }

  }
}

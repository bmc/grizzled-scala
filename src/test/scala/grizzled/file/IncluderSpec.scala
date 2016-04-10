package grizzled.file

import java.io.{FileWriter, PrintWriter, File}
import java.net.URL

import org.http4s.server.Server
import org.http4s.{Response, Request}

import org.scalatest.{FlatSpec, Matchers}

import grizzled.file.{util => FileUtil}
import FileUtil.withTemporaryDirectory
import grizzled.util.withResource

import scala.io.Source
import scala.util.Success
import scalaz.concurrent.Task

/** Created by bmc on 4/10/16
  *
  */
class IncluderSpec extends FlatSpec with Matchers {

  private val HTTPServerPort = 10000

  "Includer" should "handle a file including another file" in {
    withTemporaryDirectory("incl") { dir =>
      val input = createFile(dir, "outer.txt",
        Array("This is a normal line.",
              """%include "inner.txt"""",
              "This is another normal line."))
      createFile(dir, "inner.txt",
        Array("Inner file line 1.",
              "Inner file line 2."))
      Includer(input.getPath).map(_.toVector) shouldBe
        Success(Vector("This is a normal line.",
                       "Inner file line 1.",
                       "Inner file line 2.",
                       "This is another normal line."))
    }
  }

  it should "handle a file including another file including another file" in {
    withTemporaryDirectory("incl") { dir =>
      val input = createFile(dir, "main.txt",
        Array("main line 1",
              """%include "inner1.txt"""",
              "main line 3"))
      createFile(dir, "inner1.txt",
        Array("inner 1.1",
              """%include "inner2.txt"""",
              "inner 1.3"))
      createFile(dir, "inner2.txt",
        Array("inner 2.1",
              "inner 2.2"))
      Includer(input.getPath).map(_.toVector) shouldBe
        Success(Vector("main line 1",
                       "inner 1.1",
                       "inner 2.1",
                       "inner 2.2",
                       "inner 1.3",
                       "main line 3"))
    }
  }

  it should "allow a file to include from an HTTP server" in {
    val server = serveHTTP("foo") {
      """foo"""
    }

    withTemporaryDirectory("incl") { dir =>
      val input = createFile(dir, "main.txt",
        Array("main line 1",
              s"""%include "http://localhost:${HTTPServerPort}/foo"""",
              "main line 3"))
      withHTTPServer(server) {
        Includer(input).map(_.toVector) shouldBe Success(
          Vector("main line 1", "foo", "main line 3")
        )
      }
    }
  }

  it should "read and include from an HTTP server" in {
    import org.http4s.dsl._

    val handler: PartialFunction[Request, Task[Response]] = {
      case GET -> Root / "foo.txt" =>
        Ok("""|line 1
              |%include "bar.txt"
              |line 3""".stripMargin)
      case GET -> Root / "bar.txt" =>
        Ok("""inside bar.txt""")
    }

    val server = serveHTTP(handler)
    withHTTPServer(server) {
      val includer = Includer(s"http://localhost:$HTTPServerPort/foo.txt").map(_.toVector)
      includer shouldBe Success(Vector("line 1",
                                      "inside bar.txt",
                                      "line 3"))
    }
  }

  it should "handle an include that uses a file:// URL" in {
    withTemporaryDirectory("incl") { dir =>
      val inner = createFile(dir, "inner1.txt",
        Array("inner 1",
              "inner 2"))

      val input = createFile(dir, "main.txt",
        Array("main line 1",
              s"""%include "file://${inner.getAbsolutePath}"""",
              "main line 3"))

      Includer(input.getPath).map(_.toVector) shouldBe
        Success(Vector("main line 1",
                       "inner 1",
                       "inner 2",
                       "main line 3"))
    }
  }

  it should "abort with an exception on a direct recursive include" in {
    withTemporaryDirectory("incl") { dir =>
      val input = createFile(dir, "main.txt", Array("""%include "main.txt""""))

      intercept[IllegalStateException] {
        Includer(input.getPath).map(_.toVector).get
      }
    }
  }

  it should "abort with an exception on an indirect recursive include" in {
    withTemporaryDirectory("incl") { dir =>
      val input = createFile(dir, "main.txt", Array("""%include "inner.txt""""))
      createFile(dir, "inner.txt", Array("""%include "main.txt""""))

      intercept[IllegalStateException] {
        Includer(input.getPath).map(_.toVector).get
      }
    }
  }

  it should "support an alternate include syntax" in {
    withTemporaryDirectory("incl") { dir =>
      val input = createFile(dir, "main.txt",
        Array("line 1", "#include 'foo.txt'", "#  include 'bar.txt'", "line 2")
      )
      val foo = createFile(dir, "foo.txt", Array("foo"))
      val bar = createFile(dir, "bar.txt", Array("bar"))

      val i = Includer(input, """^#\s*include\s*'(.*)'\s*$""".r)
      i.map(_.toVector) shouldBe Success(Vector("line 1", "foo", "bar", "line 2"))
    }
  }

  it should "support an alternate nesting level" in {
    withTemporaryDirectory("incl") { dir =>
      val input = createFile(dir, "main.txt",
        Array("line 1", """%include "foo.txt"""", "line 2")
      )
      val foo = createFile(dir, "foo.txt", Array("""%include "bar.txt""""))
      val bar = createFile(dir, "bar.txt", Array("bar"))

      // The default should work.
      Includer(input).map(_.toVector) shouldBe
        Success(Vector("line 1", "bar", "line 2"))

      intercept[IllegalStateException] {
        Includer(input, 1).map(_.toVector).get
      }
    }
  }

  // --------------------------------------------------------------------------
  // Helpers
  // --------------------------------------------------------------------------

  /** Create a file in a given directory, with the specified contents.
    *
    * @param dir    the directory
    * @param file   the file
    * @param lines  the lines in the file
    *
    * @return the created file
    */
  private def createFile(dir: File, file: String, lines: Array[String]): File = {
    val path = FileUtil.joinPath(dir.getPath, file)
    withResource(new PrintWriter(new FileWriter(path))) { w =>
      lines.foreach(w.println)
    }
    new File(path)
  }

  /** Create an HTTP server on "localhost", listening to the default port
    * (HTTPServerPort) that responds to the specified path with the specified
    * string.
    *
    * Use with `withHTTPServer()` to ensure server shutdown.
    *
    * @param path   the path the server should response to. This is a relative
    *               path. e.g., Specify "foo" to have the server respond to
    *               "/foo".
    * @param result the string the server should send back when the path is
    *               requested
    *
    * @return the `Server` object, already running.
    */
  private def serveHTTP(path: String)(result: => String): Server = {
    import org.http4s.dsl._
    serveHTTP {
      case GET -> Root / path =>
        Ok(result)
    }
  }

  /** Create an HTTP server on "localhost", listening to the default port
    * (HTTPServerPort) that uses the specified http4s partial function to
    * define how to respond.
    *
    * Use with `withHTTPServer()` to ensure server shutdown.
    *
    * @param pf  the partial function representing the body of the server
    *
    * @return the `Server` object, already running.
    */
  private def serveHTTP(pf: PartialFunction[Request, Task[Response]]): Server = {
    import org.http4s._
    import org.http4s.server.blaze._

    val service = HttpService(pf)
    val builder = BlazeBuilder.bindHttp(HTTPServerPort, "localhost")
                              .mountService(service)
    builder.run
  }

  /** Given an already running HTTP server, execute the specified code, and
    * shut the server down.
    *
    * @param server The already-running server. (See `serveHTTP`.)
    * @param code   The code to run before shutting the server down.
    */
  private def withHTTPServer(server: Server)(code: => Unit): Unit = {
    try {
      code
    }
    finally {
      server.shutdownNow()
    }
  }

}

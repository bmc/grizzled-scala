package grizzled.testutil

import java.io.{PrintWriter, OutputStreamWriter}
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.Try

/** Brain dead HTTP server, solely for testing. I could use an external
  * package, but doing it this way reduces external dependencies, since I'm
  * only using a lightweight HTTP server in ScalaTest tests.
  *
  * Restrictions:
  * - Only handles text/plain, because that's all the tests need.
  * - Doesn't scale (and doesn't really need to).
  * - Only sends 200 (OK) and 404 (Not Found).
  * - Doesn't bother with client-friendly headers like "Last-Modified".
  * - EXTREMELY minimalist.
  */
object BrainDeadHTTP {

  /** Given an instantiated, but not running, Brain Dead HTTP server, execute
    * the specified code (which presumably queries the server), and shut the
    * server down.
    *
    * @param server The not-running server.
    * @param code   The code to run before shutting the server down.
    */
  def withHTTPServer(server: Server)(code: => Unit): Unit = {
    try {
      server.start()
      code
    }
    finally {
      server.stop()
    }
  }

  /** Defines a handler for a request.
    *
    * @param path    the path, minus any leading "/"
    * @param handle  the handler. Takes a Request and returns a Response.
    */
  case class Handler(path: String, handle: Request => Response)

  /** The incoming request. Minimalist.
    *
    * @param ipAddress the client's IP address
    */
  case class Request(ipAddress: InetAddress)

  /** HTTP result, from a handler
    *
    * @param code    the code
    * @param content the text content, if any
    */
  case class Response(code: ResultCode.Value, content: Option[String] = None)

  /** Minimalist. Only the ones we're using.
    */
  object ResultCode extends Enumeration {
    type ResultCode = Value

    val OK       = Value("200 OK")
    val NotFound = Value("404 Not Found")
  }

  import ResultCode._

  /** The actual server, which operates purely at the socket level.
    *
    * @param bindPort Bind to the specified TCP port
    * @param handlers Sequence of handlers to process requests
    */
  class Server(bindPort: Int, handlers: Seq[Handler]) {

    /** Create a server with only a single handler.
      *
      * @param bindPort the port on which to listen
      * @param handler  the handler
      */
    def this(bindPort: Int, handler: Handler) = this(bindPort, Vector(handler))

    import java.net._

    private val loopback = InetAddress.getLoopbackAddress
    private val socket = new ServerSocket(bindPort, 5, loopback)

    /** Start the server.
      */
    def start(): Unit = {
      new Thread() {
        override def run(): Unit = {
          try {
            while (! socket.isClosed) {
              acceptAndHandle(socket)
            }
          }
          catch {
            case _: SocketException =>
          }
        }
      }
      .start()
    }

    /** Stop the server.
      */
    def stop(): Unit = {
      socket.close()
    }

    private def acceptAndHandle(socket: ServerSocket): Unit = {
      val connection = socket.accept()
      handle(connection).andThen { case t: Try[Unit] => connection.close() }
    }

    private val GetRequest = """^GET\s+(\S+)\s+HTTP/1.\d\s*$""".r
    private def handle(connection: Socket): Future[Unit] = {
      Future {
        val out = new PrintWriter(
          new OutputStreamWriter(connection.getOutputStream)
        )

        Source.fromInputStream(connection.getInputStream)
              .getLines
              .take(1)
              .toList match {
          case GetRequest(path) :: Nil =>
            processGet(path, connection.getInetAddress, out)

          case _ => issue404(out)
        }
      }
    }

    private def issue404(w: PrintWriter): Unit = {
      sendResponse(w, new Response(NotFound, None))
    }

    private def processGet(path:     String,
                           clientIP: InetAddress,
                           w:        PrintWriter): Unit = {
      val handler = handlers.find { h => s"/${h.path}" == path }

      if (handler.isDefined) {
        sendResponse(w, handler.get.handle(Request(clientIP)))
      }
      else {
        issue404(w)
      }
    }

    private val df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss zzz")
    df.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))

    private def sendResponse(w: PrintWriter, result: Response): Unit = {
      val contentLength = result.content.map(_.length).getOrElse(0)
      w.println(
        s"""|HTTP/1.1 ${result.code}
            |Server: BrainDeadHTTP/0.0.1
            |Date: ${df.format(new Date)}
            |Content-Type: text/plain
            |Content-Length: $contentLength
            |""".stripMargin
      )

      result.content.foreach(w.print)
      w.flush()
    }
  }
}

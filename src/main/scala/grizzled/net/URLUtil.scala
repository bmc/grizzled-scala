package grizzled.net

import grizzled.file.{util => FileUtil}
import java.io.{File, IOException, InputStream, OutputStream}
import java.net.{URL => JavaURL}
import java.nio.file.Paths

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

/** URL-related utility methods.
  */
object URLUtil {
  import grizzled.util.Implicits.RichTry

  private lazy val ExtRegexp = """^(.*)(\.[^.]+)$""".r

  /** Download the specified URL to a file. The name of the file is
    * a generated temporary file name. If you want to control the file name,
    * use one of the other versions of this function.
    *
    * @param url  The string containing the URI/URL
    * @param ctx  The concurrent execution content to use
    *
    * @return a `Future` of the file to which the download was written
    */
  def download(url: String)(implicit ctx: ExecutionContext): Future[File] = {
    for { u   <- URL(url).toFuture
          res <- download(u)(ctx) }
      yield res
  }

  /** Download the specified URL to a file. The name of the file is
    * a generated temporary file name. If you want to control the file name,
    * use one of the other versions of this function.
    *
    * @param url  The `java.net.URL`
    * @param ctx  The concurrent execution content to use
    *
    * @return a `Future` of the file to which the download was written
    */
  def download(url: JavaURL)(implicit ctx: ExecutionContext): Future[File] = {
    download(URL(url))(ctx)
  }

  /** Download the specified URL to a file. The name of the file is
    * a generated temporary file name. If you want to control the file name,
    * use one of the other versions of this function.
    *
    * @param url  The `grizzled.net.URL`
    * @param ctx  The concurrent execution content to use
    *
    * @return a `Future` of the file to which the download was written
    */
  def download(url: URL)
              (implicit ctx: ExecutionContext): Future[File] = {
    for { output <- getOutputFile(url).toFuture
          _      <- download(url, output.getPath)(ctx) }
      yield output
  }

  /** Download the specified URL to a file.
    *
    * @param url      The string containing the URI/URL
    * @param pathOut  Path of file to receive the output. If the file
    *                 exists, it is overwritten. If the file does not
    *                 exist, it is created. If any of the directories in
    *                 the path do not exist, they are created.
    * @param ctx      The concurrent execution content to use
    *
    * @return a `Future` of the path
    */
  def download(url: String, pathOut: String)
              (implicit ctx: ExecutionContext): Future[String] = {
    for { u <- URL(url).toFuture
          f <- download(u, new File(pathOut))(ctx) }
    yield f.getPath
  }

  /** Download the specified URL to a file.
    *
    * @param url      The `java.net.URL`
    * @param pathOut  Path of file to receive the output. If the file
    *                 exists, it is overwritten. If the file does not
    *                 exist, it is created. If any of the directories in
    *                 the path do not exist, they are created.
    * @param ctx      The concurrent execution content to use
    *
    * @return a `Future` of the path
    */
  def download(url: JavaURL, pathOut: String)
              (implicit ctx: ExecutionContext): Future[String] = {
    download(URL(url), new File(pathOut))(ctx).map { _.getPath }
  }

  /** Download the specified URL to a file.
    *
    * @param url      The `grizzled.net.URL`
    * @param pathOut  Path of file to receive the output. If the file
    *                 exists, it is overwritten. If the file does not
    *                 exist, it is created. If any of the directories in
    *                 the path do not exist, they are created.
    * @param ctx      The concurrent execution content to use
    *
    * @return a `Future` of the path
    */
  def download(url: URL, pathOut: String)
              (implicit ctx: ExecutionContext): Future[String] = {
    download(url, new File(pathOut))(ctx).map { f => f.getPath }
  }

  /** Download the specified URL to a file.
    *
    * @param url      The string containing the URI/URL
    * @param pathOut  File to receive the output. If the file exists, it
    *                 is overwritten. If the file does not exist, it is
    *                 created. If any of the directories in the path do not
    *                 exist, they are created.
    * @param ctx      The concurrent execution content to use
    *
    * @return a `Future` of the path
    */
  def download(url: String, pathOut: File)
              (implicit ctx: ExecutionContext): Future[String] = {
    for { u <- URL(url).toFuture
          _ <- download(u, pathOut)(ctx) }
      yield pathOut.getPath
  }

  /** Download the specified URL to a file.
    *
    * @param url      The URL
    * @param pathOut  File to receive the output. If the file exists, it
    *                 is overwritten. If the file does not exist, it is
    *                 created. If any of the directories in the path do not
    *                 exist, they are created.
    * @param ctx      The concurrent execution content to use
    *
    * @return A `Future` of the path.
    */
  def download(url: URL, pathOut: File)
              (implicit ctx: ExecutionContext): Future[File] = {
    import java.io.{BufferedInputStream, BufferedOutputStream}
    import java.io.FileOutputStream
    import grizzled.io.Implicits.RichInputStream
    import grizzled.file.Implicits.GrizzledFile

    def validateAndGetParentDir(path: File): Future[File] = {
      if (path.isDirectory) {
        Future.failed(
          new IOException(
            s"""Output file "$pathOut" exists and is a directory."""
          )
        )
      }
      else {
        val dir = pathOut.dirname
        if ((! dir.exists) && (! dir.mkdirs())) {
          Future.failed(new IOException(
            s"Can't create target directory '$dir' or one of its parents."
          ))
        }
        else {
          Future.successful(dir)
        }
      }
    }

    def openInputAndOutput(url: URL, path: File):
      Future[(InputStream, OutputStream)] = {

      for { urlIn <- url.openStream().toFuture
            in    <- Try { new BufferedInputStream(urlIn) }.toFuture
            out   <- Try { new BufferedOutputStream(
                             new FileOutputStream(pathOut)) }.toFuture }
        yield (in, out)
    }

    def doCopy(in: InputStream, out: OutputStream): Future[Unit] = {
      Future {
        in.copyTo(out)
      }
    }

    for { parentDir <- validateAndGetParentDir(pathOut)
          (in, out) <- openInputAndOutput(url, pathOut)
          _         <- doCopy(in, out) }
      yield {
        out.close()
        in.close()

        pathOut
      }
  }

  /** Execute a block of code with a downloaded, temporary file.
    * The specified URL is downloaded to a file, the file is passed
    * to the block, and when the block exits, the file is removed.
    *
    * '''Note''': This function is synchronous, waiting for the underlying
    * `Future` objects using a timeout specified by the caller. For
    * asynchronous operations, use `download()`.
    *
    * @param url     the URL
    * @param timeout the timeout, as a duration.
    * @param block   the block to execute
    * @param ctx     The concurrent execution content to use
    *
    * @return `Failure(error)` on error. `Success(t)`, where `t` is what the
    *         block returns, on success.
    */
  def withDownloadedFile[T](url: String, timeout: Duration)(block: File => T)
                           (implicit ctx: ExecutionContext): Try[T] = {
    for { u   <- URL(url)
          res <- withDownloadedFile(u, timeout)(block) }
      yield res
  }

  /** Execute a block of code with a downloaded, temporary file.
    * The specified URL is downloaded to a file, the file is passed
    * to the block, and when the block exits, the file is removed.
    *
    * '''Note''': This function is synchronous, waiting for the underlying
    * `Future` objects using a timeout specified by the caller. For
    * asynchronous operations, use `download()`.
    *
    * @param url     the `java.net.URL`
    * @param timeout the timeout, as a duration.
    * @param block   the block to execute
    * @param ctx     The concurrent execution content to use
    *
    * @return `Failure(error)` on error. `Success(t)`, where `t` is what the
    *         block returns, on success.
    */
  def withDownloadedFile[T](url: JavaURL, timeout: Duration)(block: File => T)
                           (implicit ctx: ExecutionContext): Try[T] = {
    withDownloadedFile(URL(url), timeout)(block)
  }

  /** Execute a block of code with a downloaded, temporary file.
    * The specified URL is downloaded to a file, the file is passed
    * to the block, and when the block exits, the file is removed.
    *
    * '''Note''': This function is synchronous, waiting for the underlying
    * `Future` objects using a timeout specified by the caller. For
    * asynchronous operations, use `download()`.
    *
    * @param url     the URL
    * @param timeout the timeout, as a duration.
    * @param block   the block to execute
    * @param ctx     The concurrent execution content to use
    *
    * @return `Failure(error)` on error. `Success(t)`, where `t` is what the
    *         block returns, on success.
    */
  def withDownloadedFile[T](url: URL, timeout: Duration)(block: File => T)
                           (implicit ctx: ExecutionContext): Try[T] = {
    val fut = download(url).map { res =>
      block(res)
    }

    Try {
      Await.result(fut, timeout)
    }
  }

  private[net] def getOutputFile(url: URL): Try[File] = {
    Try {
      url.path.map { pathStr =>
        // URL paths are always /-separated
        val extension = pathStr match {
          case ExtRegexp(pathNoExt, ext) => ext
          case _                         => ".dat"
        }

        pathStr.last match {
          case '/' =>
            File.createTempFile("urldownload", extension)

          case _   =>
            new File(FileUtil.joinPath(System.getProperty("java.io.tmpdir"),
                                       FileUtil.basename(pathStr)))
        }
      }
      .getOrElse {
        File.createTempFile("urldownload", ".dat")
      }
    }
  }
}

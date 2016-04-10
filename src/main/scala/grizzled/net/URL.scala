package grizzled.net

import java.io.InputStream

import scala.util.Try

/** Convenient Scala case-class wrapper for a `java.net.URL`. This class
  * doesn't include all the capabilities. For example, it lacks the equivalent
  * of `getContent()`, as that's better handled through other means.
  *
  * @param protocol   the protocol
  * @param host       the host, if defined
  * @param port       the port, if defined
  * @param path       the path
  */
case class URL(protocol: String,
               host:     Option[String],
               port:     Option[Int],
               path:     Option[String]) {

  /** The underlying `java.net.URL`
    */
  val javaURL = new java.net.URL(protocol,
                                 host.orNull,
                                 port.getOrElse(-1),
                                 path.orNull)


  /** The coded authority for this URI.
    *
    * @return the authority, if any
    */
  def authority = Option(javaURL.getAuthority)

  /** The anchor. This is the same as `getRef()` on a `java.net.URL`.
    *
    * @return the anchor, if any
    */
  val anchor = Option(javaURL.getRef)

  /** Open an input stream to the URL.
    *
    * @return `Success(stream)` or `Failure(Exception)`
    */
  def openStream(): Try[InputStream] = Try {
    javaURL.openStream()
  }
}

/** Companion object, adding some functions that aren't available in the
  * generated one.
  */
object URL {
  /** Construct a URL from a `java.net.URL`.
    *
    * @param url the `java.net.URL`
    */
  def apply(url: java.net.URL): URL = {
    URL(protocol = url.getProtocol,
        host     = Option(url.getHost),
        port     = if (url.getPort < 0) None else Some(url.getPort),
        path     = Option(url.getPath))
  }

  /** Construct a URL from a string.
    *
    * @param spec the string specification
    * @return `Success(URL)` or `Failure(Exception)`
    */
  def apply(spec: String): Try[URL] = Try { URL(new java.net.URL(spec)) }
}

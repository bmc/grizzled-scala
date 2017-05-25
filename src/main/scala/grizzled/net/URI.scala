package grizzled.net

import scala.util.Try

/** Convenient Scala case-class wrapper for a `java.net.URI`.
  *
  * @param scheme     the scheme, if defined
  * @param userInfo   the user info, if defined
  * @param host       the host, if defined
  * @param port       the port, if defined
  * @param path       the path, if defined
  * @param query      the query string, if defined
  * @param fragment   the fragment, if defined
  */
final case class URI(scheme:    Option[String],
                     userInfo:  Option[String],
                     host:      Option[String],
                     port:      Option[Int],
                     path:      Option[String],
                     query:     Option[String] = None,
                     fragment:  Option[String] = None) {

  /** The underlying `java.net.URI`.
    */
  val javaURI = new java.net.URI(scheme.orNull,
                                 userInfo.orNull,
                                 host.orNull,
                                 port.getOrElse(-1),
                                 path.orNull,
                                 query.orNull,
                                 fragment.orNull)

  /** The coded authority for this URI.
    *
    * @return the authority, if any
    */
  def authority = Option(javaURI.getAuthority)

  /** Resolve the given URI against this URI.
    *
    * @param uri  the other URI
    *
    * @return `Success(URI)` or `Failure(Exception)`
    */
  def resolve(uri: URI): Try[URI] = Try { URI(javaURI.resolve(javaURI)) }

  /** Construct a new URI by parsing the given string and resolving it against
    * this URI.
    *
    * @param str  the string
    *
    * @return `Success(URI)` or `Failure(Exception)`
    */
  def resolve(str: String): Try[URI] = Try { URI(javaURI.resolve(str)) }

  /** Relativize another URI against this one.
    *
    * @param uri  the other URI
    *
    * @return `Success(URI)` or `Failure(Exception)`
    */
  def relativize(uri: URI): Try[URI] = Try {
    URI(javaURI.relativize(uri.javaURI))
  }

  /** Determine whether this URI is absolute or not.
    *
    * @return true if absolute, false if not
    */
  val isAbsolute = javaURI.isAbsolute

  /** Determine whether this URI is opaque or not.
    *
    * @return true if opaque, false if not
    */
  val isOpaque = javaURI.isOpaque

  /** Normalize the URI's path, returning a new URI.
    *
    * @return a possibly normalized URI.
    */
  def normalize = URI(javaURI.normalize)

  /** Get the URI string representation of this URI (i.e., the string
    * you could paste into a browser). Contrast this function with
    * `toString()`, which gets the string representation of the object
    * and its fields.
    *
    * @return the string
    */
  def toExternalForm = javaURI.toString

  /** Convert to a URL object.
    *
    * @return `Success(URL)` or `Failure(Exception)`
    */
  def toURL: Try[URL] = Try { URL(javaURI.toURL) }
}

/** Companion object, adding some functions that aren't available in the
  * generated one.
  */
object URI {
  /** Construct a URI from a `java.net.URI`.
    *
    * @param uri the `java.net.URI`
    */
  def apply(uri: java.net.URI): URI = {
    URI(scheme   = Option(uri.getScheme).filter(_.length > 0),
        userInfo = Option(uri.getUserInfo).filter(_.length > 0),
        host     = Option(uri.getHost).filter(_.length > 0),
        port     = if (uri.getPort < 0) None else Some(uri.getPort),
        path     = Option(uri.getPath).filter(_.length > 0),
        query    = Option(uri.getQuery).filter(_.length > 0),
        fragment = Option(uri.getFragment).filter(_.length > 0))
  }

  /** Construct a URI from a string.
    *
    * @param uriString the string
    * @return `Success(URI)` on success, or `Failure(Exception)`
    */
  def apply(uriString: String): Try[URI] = {
    Try {
      apply(new java.net.URI(uriString))
    }
  }

}

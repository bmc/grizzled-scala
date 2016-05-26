package grizzled.net

import java.net.{MalformedURLException, URISyntaxException}

import grizzled.BaseSpec

import scala.io.Source

class URLSpec extends BaseSpec {
  "URL" should "properly parse a simple HTTP URL string" in {
    val r = URL("http://localhost/foo/bar")
    r shouldBe success
    val u = r.get
    u.host shouldBe Some("localhost")
    u.protocol shouldBe "http"
    u.path shouldBe Some("/foo/bar")
    u.port shouldBe None
    u.query shouldBe None
    u.fragment shouldBe None
  }

  it should "return 80 as the default HTTP port" in {
    val r = URL("http://localhost/foo/bar")
    r shouldBe success
    val u = r.get
    u.defaultPort shouldBe Some(80)
  }

  it should "properly parse a query string from an HTTP URL" in {
    val r = URL("http://localhost/foo.html?q=hello&lang=en_US")
    r shouldBe success
    val u = r.get
    u.query shouldBe Some("q=hello&lang=en_US")
  }

  it should "properly parse a fragment from an HTTP URL" in {
    val r = URL("http://localhost/foo.html#section1")
    r shouldBe success
    val u = r.get
    u.fragment shouldBe Some("section1")
  }

  it should "properly parse a port from an HTTP URL" in {
    val r = URL("http://localhost:9988/foo.html#section1")
    r shouldBe success
    val u = r.get
    u.port shouldBe Some(9988)
  }

  it should "properly parse user info from an HTTP URL" in {
    val r = URL("http://user@localhost/foo.html#section1")
    r shouldBe success
    val u = r.get
    u.userInfo shouldBe Some("user")
  }

  it should "properly parse user and password info from an HTTP URL" in {
    val r = URL("http://user:mypass@localhost/foo.html#section1")
    r shouldBe success
    val u = r.get
    u.userInfo shouldBe Some("user:mypass")
  }

  it should "properly handle an HTTPS URL" in {
    val us = "https://user:mypass@localhost/foo.zip"
    val r = URL(us)
    r shouldBe success
    val u = r.get
    u.userInfo shouldBe Some("user:mypass")
    u.protocol shouldBe "https"
    u.host shouldBe Some("localhost")
    u.port shouldBe None
    u.path shouldBe Some("/foo.zip")
    u.defaultPort shouldBe Some(443)
    u.toExternalForm shouldBe us
  }

  it should "properly handle an FTP URL" in {
    val us = "ftp://user:mypass@localhost/foo.zip"
    val r = URL(us)
    r shouldBe success
    val u = r.get
    u.userInfo shouldBe Some("user:mypass")
    u.protocol shouldBe "ftp"
    u.host shouldBe Some("localhost")
    u.port shouldBe None
    u.path shouldBe Some("/foo.zip")
    u.defaultPort shouldBe Some(21)
    u.toExternalForm shouldBe us
  }

  it should "properly handle a file URL" in {
    val r = URL("file:///this/is/a/path")
    r shouldBe success
    val u = r.get
    u.userInfo shouldBe None
    u.protocol shouldBe "file"
    u.host shouldBe None
    u.port shouldBe None
    u.path shouldBe Some("/this/is/a/path")
    u.defaultPort shouldBe None
    u.toExternalForm shouldBe "file:/this/is/a/path"
  }

  it should "properly handle a jar URL" in {
    val us = "jar:file:///this/is/a/path.jar!/foo/bar.class"
    val r = URL(us)
    r shouldBe success
    val u = r.get
    u.userInfo shouldBe None
    u.protocol shouldBe "jar"
    u.host shouldBe None
    u.port shouldBe None
    u.path shouldBe Some("file:///this/is/a/path.jar!/foo/bar.class")
    u.defaultPort shouldBe None
    u.toExternalForm shouldBe us
  }

  it should "abort on a bad protocol" in {
    intercept[MalformedURLException] {
      URL("argh:/foo/bar").get
    }
  }

  it should "abort on a bad port" in {
    intercept[MalformedURLException] {
      URL("http://localhost:hello/foo").get
    }
  }

  it should "abort if there's no path in a file URL" in {
    intercept[URISyntaxException] {
      URL("file:").get
    }
  }

  it should "abort if there's no host in an HTTP URL" in {
    intercept[URISyntaxException] {
      URL("http://").get
    }
  }

  "URL.openStream" should "open a readable InputStream for contents" in {
    import grizzled.testutil.BrainDeadHTTP._

    val server = new Server(Handler("foo.txt", { _ =>
      Response(ResponseCode.OK, Some("foo\n"))
    }))

    withHTTPServer(server) { _ =>
      val r = URL(s"http://localhost:${server.bindPort}/foo.txt")
      r shouldBe success
      val url = r.get
      val t = url.openStream()
      t shouldBe success
      val source = Source.fromInputStream(t.get)
      source.mkString shouldBe "foo\n"
    }
  }
}

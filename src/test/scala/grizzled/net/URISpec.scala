package grizzled.net

import java.net.{MalformedURLException, URISyntaxException}

import grizzled.BaseSpec

import scala.util.Success

class URISpec extends BaseSpec {
  "URI" should "properly parse a simple HTTP URL string" in {
    val r = URI("http://localhost/foo/bar")
    r shouldBe success
    val u = r.get
    u.host shouldBe Some("localhost")
    u.scheme shouldBe Some("http")
    u.path shouldBe Some("/foo/bar")
    u.port shouldBe None
    u.query shouldBe None
    u.fragment shouldBe None
  }

  it should "properly parse a query string from an HTTP URL" in {
    val r = URI("http://localhost/foo.html?q=hello&lang=en_US")
    r shouldBe success
    val u = r.get
    u.query shouldBe Some("q=hello&lang=en_US")
  }

  it should "properly parse a fragment from an HTTP URL" in {
    val r = URI("http://localhost/foo.html#section1")
    r shouldBe success
    val u = r.get
    u.fragment shouldBe Some("section1")
  }

  it should "properly parse a port from an HTTP URL" in {
    val r = URI("http://localhost:9988/foo.html#section1")
    r shouldBe success
    val u = r.get
    u.port shouldBe Some(9988)
  }

  it should "properly parse user info from an HTTP URL" in {
    val r = URI("http://user@localhost/foo.html#section1")
    r shouldBe success
    val u = r.get
    u.userInfo shouldBe Some("user")
  }

  it should "properly parse user and password info from an HTTP URL" in {
    val r = URI("http://user:mypass@localhost/foo.html#section1")
    r shouldBe success
    val u = r.get
    u.userInfo shouldBe Some("user:mypass")
  }

  it should "properly handle an HTTPS URL" in {
    val us = "https://user:mypass@localhost/foo.zip"
    val r = URI(us)
    r shouldBe success
    val u = r.get
    u.userInfo shouldBe Some("user:mypass")
    u.scheme shouldBe Some("https")
    u.host shouldBe Some("localhost")
    u.port shouldBe None
    u.path shouldBe Some("/foo.zip")
    u.toExternalForm shouldBe us
  }

  it should "properly handle an FTP URL" in {
    val us = "ftp://user:mypass@localhost/foo.zip"
    val r = URI(us)
    r shouldBe success
    val u = r.get
    u.userInfo shouldBe Some("user:mypass")
    u.scheme shouldBe Some("ftp")
    u.host shouldBe Some("localhost")
    u.port shouldBe None
    u.path shouldBe Some("/foo.zip")
    u.toExternalForm shouldBe us
  }

  it should "properly handle a file URL" in {
    val r = URI("file:///this/is/a/zipPath")
    r shouldBe success
    val u = r.get
    u.userInfo shouldBe None
    u.scheme shouldBe Some("file")
    u.host shouldBe None
    u.port shouldBe None
    u.path shouldBe Some("/this/is/a/zipPath")
    u.toExternalForm shouldBe "file:/this/is/a/zipPath"
  }

  it should "properly handle a plain file (with no protocol)" in {
    val path = "/tmp/foo/bar/x.txt"
    val r = URI(path)
    r shouldBe success
    val u = r.get
    u.userInfo shouldBe None
    u.scheme shouldBe None
    u.host shouldBe None
    u.port shouldBe None
    u.path shouldBe Some(path)
    u.toExternalForm shouldBe path
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

  it should "abort if there's no zipPath in a file URL" in {
    intercept[URISyntaxException] {
      URL("file:").get
    }
  }

  it should "abort if there's no host in an HTTP URL" in {
    intercept[URISyntaxException] {
      URL("http://").get
    }
  }

  "URI.relativize" should "handle HTTP URLs that are hierarchically related" in {
    // "base" must be a prefix of "other"
    val base = "http://www.example.org:80/main/"
    val other = s"$base/foo/bar.txt"

    val rb = URI(base)
    val ro = URI(other)

    rb shouldBe success
    ro shouldBe success

    val (ub, uo) = (rb.get, ro.get)

    val rr = ub.relativize(uo)
    rr shouldBe success
    val ur = rr.get

    ur.scheme shouldBe None
    ur.userInfo shouldBe None
    ur.host shouldBe None
    ur.port shouldBe None
    ur.path shouldBe Some("foo/bar.txt")
  }

  it should "handle HTTP URLs that are in the same 'directory'" in {
    val base = "http://www.example.org:80/main/index.html"
    val other = "http://www.example.org:80/main/foo/bar.txt"

    val rb = URI(base)
    val ro = URI(other)

    rb shouldBe success
    ro shouldBe success

    val (ub, uo) = (rb.get, ro.get)

    val rr = ub.relativize(uo)
    rr shouldBe success
    val ur = rr.get

    uo.path shouldBe ur.path
  }

  it should "handle HTTP URLs that aren't related at all" in {
    val base = "http://www.example.org:80/main/index.html"
    val other = "http://www.example.org:80/foo/bar/baz.html"

    val rb = URI(base)
    val ro = URI(other)

    rb shouldBe success
    ro shouldBe success

    val (ub, uo) = (rb.get, ro.get)

    val rr = ub.relativize(uo)
    rr shouldBe success
    val ur = rr.get

    uo.path shouldBe ur.path
  }

  it should "handle HTTP URLs that aren't on the same host" in {
    val base = "http://www.example.org:80/main/"
    val other = s"http://www.example.com/foo/bar/baz.html"

    val rb = URI(base)
    val ro = URI(other)

    rb shouldBe success
    ro shouldBe success

    val (ub, uo) = (rb.get, ro.get)

    val rr = ub.relativize(uo)
    rr shouldBe success
    val ur = rr.get

    uo.path shouldBe ur.path
  }

  it should "handle file URLs that are hierarchically related" in {
    // "base" must be a prefix of "other"
    val base = "file:/Users/bmc/tmp"
    val other = s"$base/CoolApp.dmg"

    val rb = URI(base)
    val ro = URI(other)

    rb shouldBe success
    ro shouldBe success

    val (ub, uo) = (rb.get, ro.get)

    val rr = ub.relativize(uo)
    rr shouldBe success
    val ur = rr.get

    ur.scheme shouldBe None
    ur.userInfo shouldBe None
    ur.host shouldBe None
    ur.port shouldBe None
    ur.path shouldBe Some("CoolApp.dmg")
  }

  "URI.resolve" should "properly resolve a simple zipPath against an HTTP URL" in {
    val base = "http://www.example.net/index.html"
    val file = "README.txt"

    val rb = URI(base)
    rb shouldBe success

    val resolved = rb.get.resolve(file)
    resolved shouldBe success

    resolved.get.toExternalForm shouldBe "http://www.example.net/README.txt"
  }

  it should "properly resolve a .. zipPath in an HTTP URL" in {
    val base = "http://www.example.net/main/index.html"
    val file = "../downloads/CoolApp.tgz"

    val rb = URI(base)
    rb shouldBe success

    val resolved = rb.get.resolve(file)
    resolved shouldBe success

    resolved.get.toExternalForm shouldBe
      "http://www.example.net/downloads/CoolApp.tgz"
  }

  "URI.isAbsolute" should "properly detect an absolute URI" in {
    val data = Array(
      "file:/foo/bar/baz.txt",
      "http://localhost:8080/foobar.txt",
      "ftp://localhost/foo/bar/baz.zip"
    )

    for (s <- data) {
      val r = URI(s)
      r shouldBe success
      r.get.isAbsolute shouldBe true
    }
  }

  it should "properly detect a relative URI" in {
    val data = Array("../foo", "bar.txt")

    for (s <- data) {
      val r = URI(s)
      r shouldBe success
      r.get.isAbsolute shouldBe false
    }
  }

  "URI.normalize" should "properly normalize a URI with relative components" in {
    val r = URI("https://secure.example.com:443/main/docs/../../index.html")
    r shouldBe success
    val n = r.get.normalize
    n.path shouldBe Some("/index.html")
    n.toExternalForm shouldBe "https://secure.example.com:443/index.html"
  }

  it should "work on a scheme-less URI" in {
    val path = "/Users/bmc/Applications"
    val r = URI(path)
    r shouldBe success
    val n = r.get.normalize
    n.scheme shouldBe None
    n.path shouldBe Some(path)
    n.toExternalForm shouldBe path
  }

  "URI.toURL" should "work with an HTTP URL" in {
    val s = "http://www.example.net/foo/bar/quux.html"
    val r1 = URI(s)
    r1 shouldBe success
    val r2 = r1.get.toURL
    r2 shouldBe success
    val url = r2.get
    url.toExternalForm shouldBe s
    url.protocol shouldBe "http"
  }

  it should "work with a file URL" in {
    val s = "file://tmp/vi293847.tmp"
    val r1 = URI(s)
    r1 shouldBe success
    val r2 = r1.get.toURL
    r2 shouldBe success
    val url = r2.get
    url.toExternalForm shouldBe s
    url.protocol shouldBe "file"
  }
}

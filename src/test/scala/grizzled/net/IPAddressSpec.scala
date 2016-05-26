package grizzled.net

import java.net.InetAddress

import grizzled.BaseSpec
import grizzled.net.Implicits._

import scala.util.{Failure, Try}

/**
 * Tests the grizzled.net functions in IPAddressSpec.scala
 */
class IPAddressSpec extends BaseSpec {
  def byte(thing: Int): Byte = thing.toByte
  def bytes(data: Int*): List[Byte] = data.map(_.toByte).toList

  // NOTE: Must use List[Byte], not Array[Byte]. Two identical arrays
  // are not "equal", according to "==". Two identical lists are.
  // (Scala array quirk.)
  val Data = List(
    // input                   expected result and expected string

    ("localhost",              List(127, 0, 0, 1), "127.0.0.1"),
    ("127.0.0.1",              List(127, 0, 0, 1), "127.0.0.1"),
    (Array(127, 0, 0, 1),      List(127, 0, 0, 1), "127.0.0.1"),
    (Array(192, 168, 2, 100),  List(192, 168, 2, 100), "192.168.2.100"),
    (Array(192, 168, 2),       List(192, 168, 2, 0), "192.168.2.0"),
    (Array(192, 167),          List(192, 167, 0, 0), "192.167.0.0"),
    (Array(192),               List(192, 0, 0, 0), "192.0.0.0"),
    (Array(192, 1, 1, 10, 3),  List(192, 1, 1, 10, 3, 0, 0, 0,
                                    0, 0, 0,  0, 0, 0, 0, 0),
                                    "c001:10a:300:0:0:0:0:0"),
    (Array(255, 255, 255, 255),List(255, 255, 255, 255), "255.255.255.255"),
    (Array(255, 255, 255, 0),  List(255, 255, 255, 0), "255.255.255.0"),
    (Array(0, 0, 0, 0),        List(0, 0, 0, 0), "0.0.0.0")
  )

  def getIPAddress(input: AnyRef): Try[IPAddress] = {
    input match {
      case s: String       => IPAddress("localhost")
      case a: Array[Int]   => IPAddress(a)
      case ab: Array[Byte] => IPAddress(ab)
      case _               => Failure(new IllegalArgumentException("oops"))
    }
  }

  "IPAddress constructors" should "behave as expected" in {

    for ((input, expected, expectedString) <- Data) {
      // Map the expected value into a list of bytes
      val mappedExpected = expected.map(_.toByte)
      val ipAddrRes = getIPAddress(input)
      ipAddrRes shouldBe success

      val ipAddr = ipAddrRes.get

      // Run the test
      ipAddr.address.toList shouldBe mappedExpected
      ipAddr.toString shouldBe expectedString
    }

    IPAddress(Nil) shouldBe failure
  }

  "IPAddress implicits" should "properly convert" in {
    // NOTE: Must use List[Byte], not Array[Byte]. Two identical arrays
    // are not "equal", according to "==". Two identical lists are.
    // (Scala array quirk.)

    for ((input, expected, expectedString) <- Data) {
      // Map the expected value into a list of bytes
      val mappedExpected = expected.map(_.toByte)

      val ipAddrRes = getIPAddress(input)
      ipAddrRes shouldBe success

      val ipAddr = ipAddrRes.get
      val jdkInetAddress: java.net.InetAddress = ipAddr
      val ipAddr2: IPAddress = jdkInetAddress

      jdkInetAddress.getAddress.toList shouldBe mappedExpected
      jdkInetAddress.getHostAddress shouldBe expectedString
      ipAddr2 shouldBe ipAddr
      ipAddr2.hashCode shouldBe ipAddr.hashCode
    }
  }

  "IPAddress.apply(InetAddress)" should "work" in {
    val inetAddress = InetAddress.getLoopbackAddress
    val ipAddress = IPAddress(inetAddress)

    ipAddress.toString shouldBe ("127.0.0.1")
  }

  "IPAddress.toInetAddress" should "produce a proper InetAddress" in {
    // NOTE: Must use List[Byte], not Array[Byte]. Two identical arrays
    // are not "equal", according to "==". Two identical lists are.
    // (Scala array quirk.)

    for ((input, expected, expectedString) <- Data) {
      // Map the expected value into a list of bytes
      val expectedBytes = expected.map(_.toByte)

      val ipAddrRes = getIPAddress(input)
      ipAddrRes shouldBe success

      val ipAddr = ipAddrRes.get
      ipAddr.toInetAddress.getAddress.toList shouldBe expectedBytes
    }
  }

  "java.net.InetAddress call-throughs" should "work on an IPAddress" in {
    val ipAddrRes1 = IPAddress(127, 0, 0, 1)
    ipAddrRes1 shouldBe success

    val ipAddr1 = ipAddrRes1.get
    ipAddr1.isLoopbackAddress shouldBe true


    val ipAddrRes2 = IPAddress(192, 168, 0, 1)
    ipAddrRes2 shouldBe success
    val ipAddr2 = ipAddrRes2.get
    ipAddr2.isLoopbackAddress shouldBe false
  }

  "IPAddress.parseAddress" should "handle a valid IPv4 address" in {
    val addresses = Array("192.168.12.0", "200.30.99.254", "127.0.0.1")
    for (a <- addresses)
      IPAddress.parseAddress(a) shouldBe success
  }

  it should "fail on a nonsense string" in {
    IPAddress.parseAddress("foobar") shouldBe failure
  }

  it should "fail on an invalid IPv4 address" in {
    IPAddress.parseAddress("256.0.0.1") shouldBe failure
  }

  it should "handle a valid IPv6 address" in {
    val addresses = Array(
      "2601:8c:4002:71d2:9838:f195:45c6:c8a5",
      "2600:1011:B12B:6647:A0E7:421A:7904:4F2B",
      "::129.144.52.38",
      "2600:1011:B12B:6647:A0E7:421A:7904:4923%eth0"
    )

    for (a <- addresses)
      IPAddress.parseAddress(a) shouldBe success
  }

  private val IPv4sAndNumbers = Array(
    ("192.168.10.20", BigInt("3232238100")),
    ("127.0.0.1", BigInt("2130706433"))
  )

  private val IPv6sAndNumbers = Array(
    ("2601:8c:4002:71d2:9838:f195:45c6:c8a5",
      BigInt("50515867248438085987383377804669667493")),
    ("2600:1011:B12B:6647:A0E7:421A:7904:4F2B",
      BigInt("50510989760090537890881087140064939819")),
    ("2A02:C7D:E19:2800:8485:1403:6CD3:D5CB",
      BigInt("55838213713482296607501816151617557963"))
  )

  "IPAddress.apply(BigInt)" should "handle a valid IPv6 address" in {
    for ((_, addrNum) <- IPv6sAndNumbers) {
      IPAddress(addrNum) shouldBe success
    }
  }

  it should "handle a valid IPv4 address" in {
    for ((_, addrNum) <- IPv4sAndNumbers) {
      IPAddress(addrNum) shouldBe success
    }
  }

  "IPAddress.toNumber" should "return valid numbers for IPv6 addresses" in {
    for ((s, expected) <- IPv6sAndNumbers) {
      val res = IPAddress.parseAddress(s)
      res shouldBe success
      IPAddress.parseAddress(s).get.toNumber shouldBe expected
    }
  }

  it should "return value numbers for IPv4 addresses" in {
    for ((s, expected) <- IPv4sAndNumbers) {
      val res = IPAddress.parseAddress(s)
      res shouldBe success
      IPAddress.parseAddress(s).get.toNumber shouldBe expected
    }
  }
}

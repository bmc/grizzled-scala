/*
  ---------------------------------------------------------------------------
  Copyright (c) 2009-2014 Brian M. Clapper. All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

  * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

  * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  * Neither the names "clapper.org", "Grizzled Scala Library", nor the
    names of its contributors may be used to endorse or promote products
    derived from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  ---------------------------------------------------------------------------
*/

import java.net.InetAddress

import org.scalatest.{FlatSpec, Matchers, FunSuite}
import grizzled.net._
import grizzled.net.Implicits._

/**
 * Tests the grizzled.net functions in inet.scala
 */
class IPAddressTest extends FlatSpec with Matchers {
  def byte(thing: Int): Byte = thing.toByte
  def bytes(data: Int*): List[Byte] = data.map(_.toByte).toList

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

  def getIPAddress(input: AnyRef): Either[String, IPAddress] = {
    input match {
      case s: String       => IPAddress("localhost")
      case a: Array[Int]   => IPAddress(a)
      case ab: Array[Byte] => IPAddress(ab)
      case _               => Left("oops")
    }
  }

  "IPAddress constructors" should "behave as expected" in {
    // NOTE: Must use List[Byte], not Array[Byte]. Two identical arrays
    // are not "equal", according to "==". Two identical lists are.
    // (Scala array quirk.)

    for ((input, expected, expectedString) <- Data) {
      // Map the expected value into a list of bytes
      val mappedExpected = expected.map(_.toByte)
      val ipAddrRes = getIPAddress(input)
      assert(ipAddrRes.isRight)

      val ipAddr = ipAddrRes.right.get

      // Run the test
      assertResult(mappedExpected, "IPAddress(" + input + ")") {
        ipAddr.address.toList
      }

      assertResult(expectedString, "IPAddress(" + input + ")") {
        ipAddr.toString
      }
    }

    val ip = IPAddress( (for (i <- 0 to 20) yield i.toByte).toList )
    assert(ip.isLeft)

    assert(IPAddress(Nil).isLeft)
  }

  "IPAddress implicits" should "properly convert" in {
    // NOTE: Must use List[Byte], not Array[Byte]. Two identical arrays
    // are not "equal", according to "==". Two identical lists are.
    // (Scala array quirk.)

    for ((input, expected, expectedString) <- Data) {
      // Map the expected value into a list of bytes
      val mappedExpected = expected.map(_.toByte)

      val ipAddrRes = getIPAddress(input)
      assert(ipAddrRes.isRight)

      val ipAddr = ipAddrRes.right.get
      val jdkInetAddress: java.net.InetAddress = ipAddr
      val ipAddr2: IPAddress = jdkInetAddress

      assertResult(mappedExpected, "IPAddress(" + input + ")") {
        jdkInetAddress.getAddress.toList
      }

      assertResult(expectedString, "IPAddress(" + input + ")") {
        jdkInetAddress.getHostAddress
      }

      assertResult(ipAddr, "IPAddress -> java.net.InetAddress -> IPAddress") {
        ipAddr2
      }

      assertResult(ipAddr.hashCode,
             "IPAddress -> java.net.InetAddress -> IPAddress") {
        ipAddr2.hashCode
      }
    }
  }

  "IPAddress.apply(InetAddress)" should "work" in {
    val inetAddress = InetAddress.getLoopbackAddress
    val ipAddress = IPAddress(inetAddress)

    ipAddress.toString shouldBe ("127.0.0.1")
  }

  "java.net.InetAddress call-throughs" should "work on an IPAddress" in {
    assertResult(true, "127.0.0.1 is loopback")  {
      val ipAddrRes = IPAddress(127, 0, 0, 1)
      assert(ipAddrRes.isRight)

      val ipAddr = ipAddrRes.right.get

      ipAddr.isLoopbackAddress
    }

    assertResult(false, "192.168.1.100 is not loopback")  {
      val ipAddrRes = IPAddress(192, 168, 0, 1)
      assert(ipAddrRes.isRight)
      val ipAddr = ipAddrRes.right.get
      ipAddr.isLoopbackAddress
    }
  }

  "IPAddress.parseAddress" should "handle a valid IPv4 address" in {
    val addresses = Array("192.168.12.0", "200.30.99.254", "127.0.0.1")
    for (a <- addresses)
      IPAddress.parseAddress(a).isRight shouldBe (true)
  }

  it should "fail on a nonsense string" in {
    IPAddress.parseAddress("foobar").isLeft shouldBe (true)
  }

  it should "fail on an invalid IPv4 address" in {
    IPAddress.parseAddress("256.0.0.1").isLeft shouldBe (true)
  }

  it should "handle a valid IPv6 address" in {
    val addresses = Array(
      "2601:8c:4002:71d2:9838:f195:45c6:c8a5",
      "2600:1011:B12B:6647:A0E7:421A:7904:4F2B",
      "::129.144.52.38",
      "2600:1011:B12B:6647:A0E7:421A:7904:4923%eth0"
    )

    for (a <- addresses)
      IPAddress.parseAddress(a).isRight shouldBe (true)
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
      IPAddress(addrNum).isRight shouldBe (true)
    }
  }

  it should "handle a valid IPv4 address" in {
    for ((_, addrNum) <- IPv4sAndNumbers) {
      IPAddress(addrNum).isRight shouldBe (true)
    }
  }

  "IPAddress.toNumber" should "return valid numbers for IPv6 addresses" in {
    for ((s, expected) <- IPv6sAndNumbers) {
      val res = IPAddress.parseAddress(s)
      res.isRight shouldBe (true)
      IPAddress.parseAddress(s).right.get.toNumber shouldBe (expected)
    }
  }

  it should "return value numbers for IPv4 addresses" in {
    for ((s, expected) <- IPv4sAndNumbers) {
      val res = IPAddress.parseAddress(s)
      res.isRight shouldBe (true)
      IPAddress.parseAddress(s).right.get.toNumber shouldBe (expected)
    }
  }
}

/*
  ---------------------------------------------------------------------------
  Copyright (c) 2009-2010 Brian M. Clapper. All rights reserved.

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

import org.scalatest.FunSuite
import grizzled.net._

/**
 * Tests the grizzled.net functions in inet.scala
 */
class IPAddressTest extends FunSuite
{
    def byte(thing: Int): Byte = thing toByte
    def bytes(data: Int*): List[Byte] = data map (_ toByte) toList

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

    def getIPAddress(input: AnyRef): IPAddress =
        input match
        {
            case s: String       => IPAddress("localhost")
            case a: Array[Int]   => IPAddress(a)
            case ab: Array[Byte] => IPAddress(ab)
            case _               => throw new AssertionError("oops")
        }

    test("IPAddress constructors")
    {
        // NOTE: Must use List[Byte], not Array[Byte]. Two identical arrays
        // are not "equal", according to "==". Two identical lists are.
        // (Scala array quirk.)

        for ((input, expected, expectedString) <- Data)
        {
            // Map the expected value into a list of bytes
            val mappedExpected = expected map (_ toByte)
            val ipAddr = getIPAddress(input)

            // Run the test
            expect(mappedExpected, "IPAddress(" + input + ")")
            {
                ipAddr.address toList
            }

            expect(expectedString, "IPAddress(" + input + ")")
            {
                ipAddr toString
            }
        }

        intercept[IllegalArgumentException]
        { 
            IPAddress( (for (i <- 0 to 20) yield i.toByte) toList )
        }

        intercept[IllegalArgumentException]
        {
            IPAddress(Nil) 
        }
    }

    test("IPAddress implicits")
    {
        // NOTE: Must use List[Byte], not Array[Byte]. Two identical arrays
        // are not "equal", according to "==". Two identical lists are.
        // (Scala array quirk.)

        for ((input, expected, expectedString) <- Data)
        {
            // Map the expected value into a list of bytes
            val mappedExpected = expected map (_ toByte)

            val ipAddr = getIPAddress(input)
            val jdkInetAddress: java.net.InetAddress = ipAddr
            val ipAddr2: IPAddress = jdkInetAddress

            expect(mappedExpected, "IPAddress(" + input + ")")
            {
                jdkInetAddress.getAddress.toList
            }

            expect(expectedString, "IPAddress(" + input + ")")
            {
                jdkInetAddress.getHostAddress
            }

            expect(ipAddr, "IPAddress -> java.net.InetAddress -> IPAddress")
            {
                ipAddr2
            }

            expect(ipAddr.hashCode, 
                   "IPAddress -> java.net.InetAddress -> IPAddress")
            {
                ipAddr2.hashCode
            }
        }
    }

    test("java.net.InetAddress call-throughs")
    {
        expect(true, "127.0.0.1 is loopback") 
        {
            IPAddress(127, 0, 0, 1) isLoopbackAddress
        }

        expect(false, "192.168.1.100 is not loopback") 
        {
            IPAddress(192, 168, 1, 100) isLoopbackAddress
        }
    }
}

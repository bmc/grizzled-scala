import org.scalatest.FunSuite
import grizzled.net._

/**
 * Tests the grizzled.net functions in inet.scala
 */
class IPAddressTest extends GrizzledFunSuite
{
    def byte(thing: Int): Byte = thing toByte
    def bytes(data: Int*): List[Byte] = data map (_ toByte) toList

    val Data = List(
        // input                   expected result and expected string

        ("localhost",              List(127, 0, 0, 1), "127.0.0.1"),
        (List(127, 0, 0, 1),       List(127, 0, 0, 1), "127.0.0.1"),
        (Array(127, 0, 0, 1),      List(127, 0, 0, 1), "127.0.0.1"),
        (List(192, 168, 2, 100),   List(192, 168, 2, 100), "192.168.2.100"),
        (Array(192, 168, 2, 100),  List(192, 168, 2, 100), "192.168.2.100"),
        (List(192, 168, 2),        List(192, 168, 2, 0), "192.168.2.0"),
        (Array(192, 168, 2),       List(192, 168, 2, 0), "192.168.2.0"),
        (List(192, 167),           List(192, 167, 0, 0), "192.167.0.0"),
        (Array(192, 167),          List(192, 167, 0, 0), "192.167.0.0"),
        (List(192),                List(192, 0, 0, 0), "192.0.0.0"),
        (Array(192),               List(192, 0, 0, 0), "192.0.0.0"),
        (List(255, 255, 255, 255), List(255, 255, 255, 255), "255.255.255.255"),
        (List(255, 255, 255, 0),   List(255, 255, 255, 0), "255.255.255.0"),
        (List(0, 0, 0, 0),         List(0, 0, 0, 0), "0.0.0.0")
    )

    def getIPAddress(input: AnyRef): IPAddress =
        input match
        {
            case s: String       => IPAddress("localhost")
            case a: Array[Int]   => IPAddress(a)
            case ab: Array[Byte] => IPAddress(ab)
            case l: List[Int]    => IPAddress(l map (_ toByte))
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

        intercept(classOf[AssertionError]) { IPAddress(bytes(1, 2, 3, 4, 5)) }
        intercept(classOf[AssertionError]) { IPAddress(Array(1, 2, 3, 4, 5)) }
        intercept(classOf[AssertionError]) { IPAddress(Nil) }
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
}

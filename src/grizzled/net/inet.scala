package grizzled.net

/**
 * Represents an IP address. This class is similar to the
 * <tt>java.net.InetAddress</tt>, but it's designed to be more intuitive and
 * easier to use from Scala. This package provides implicit converters to make
 * this class compatible with <tt>java.net.InetAddress</tt>.
 */
class IPAddress(val address: Array[Byte])
{
    require(address.length == 4)

    /**
     * Return a printable version of this IP address.
     *
     * @return the printable version
     */
    override def toString: String = 
    {
        // Bytes are represented as signed values in Scala. Network addresses
        // use unsigned bytes. Hence the tomfoolery.

        (address map ((x: Byte) => 0 | (x & 0xff))) mkString "."
    }

    /**
     * Overloaded "==" method to test for equality.
     *
     * @param other  object against which to test this object
     *
     * @return <tt>true</tt> if equal, <tt>false</tt> if not
     */
    override def equals(other: Any) =
        other match
        {
            case that: IPAddress =>
                that.address.toList == this.address.toList
            case _ =>
                false
        }

    /**
     * Overloaded hash method: Ensures that two <tt>IPAddress</tt> objects
     * that represent the same IP address have the same hash code.
     *
     * @return the hash code
     */
    override def hashCode: Int = address.toList.hashCode
}

/**
 * Companion object to <tt>IPAddress</tt> class.
 */
object IPAddress
{
    /**
     * Singleton <tt>IPAddress</tt> for the local loop address.
     */
    final val Localhost = IPAddress(Array(127, 0, 0, 1))

    /**
     * Create an <tt>IPAddress</tt>, given an array of bytes representing
     * the address. The array must contain between 1 and 4 byte values. If
     * the array has fewer than four values, it will be padded with 0s. If
     * the array has more than 4 values, this method will throw an
     * assertion error.
     *
     * @param addr  the address
     *
     * @return the <tt>IPAddress</tt>
     */
    def apply(addr: Array[Byte]): IPAddress =
        IPAddress(addr toList)

    /**
     * Create an <tt>IPAddress</tt>, given an array of integers
     * representing the address. The array must contain between 1 and 4
     * values. Each integer is truncated to a byte before being used. If
     * the array has fewer than four values, it will be padded with 0s. If
     * the array has more than 4 values, this method will throw an
     * assertion error. Example of use:
     *
     * <blockquote>
     * <pre>val ip = IPAddress(Array(192, 168, 1, 100))</pre>
     * </blockquote>
     *
     * @param addr  the address
     *
     * @return the corresponding <tt>IPAddress</tt> object.
     */
    def apply(addr: Array[Int]): IPAddress = 
        IPAddress(addr map (_ toByte))

    /**
     * Create an <tt>IPAddress</tt>, given 1 to 4 integer arguments. If fewer
     * than 4 arguments are given, the missing arguments default to 0.
     *
     * @param addr  the bytes (as integers) of the address
     *
     * @return the <tt>IPAddress</tt>
     */
    def apply(addr: Int*): IPAddress =
        IPAddress(addr map (_ toByte) toList)

    /**
     * Create an <tt>IPAddress</tt>, given a list of bytes representing the
     * address. The list must contain between 1 and 4 byte values. If the
     * list has fewer than four values, it will be padded with 0s. If the
     * list has more than 4 values, this method will throw an assertion
     * error.
     *
     * @param address  the list of address values
     *
     * @return the <tt>IPAddress</tt>
     */
    def apply(address: List[Byte]): IPAddress =
    {
        val zeroByte = 0 toByte
        val addrArray: Array[Byte] = address match
        {
            case List(a, b, c, d) => Array(a, b, c, d)
            case List(a, b, c)    => Array(a, b, c, zeroByte)
            case List(a, b)       => Array(a, b, zeroByte, zeroByte)
            case List(a)          => Array(a, zeroByte, zeroByte, zeroByte)

            case _                => throw new AssertionError(
                                         "List \"" + (address mkString ",") +
                                         "\": invalid length")
        }

        new IPAddress(addrArray)
    }

    /**
     * Create an <tt>IPAddress</tt>, given a host name.
     *
     * @param host  the host name
     *
     * @return the corresponding <tt>IPAddress</tt> object.
     *
     * @throws java.net.UnknownHostException unknown host
     */
    def apply(host: String): IPAddress =
        new IPAddress(java.net.InetAddress.getByName(host).getAddress)

    /**
     * Implicitly converts a <tt>java.net.InetAddress</tt> to an
     * <tt>IPAddress</tt>.
     *
     * @param addr  the <tt>java.net.InetAddress</tt>
     *
     * @return the corresponding <tt>IPAddress</tt>
     */
    implicit def inetToIPAddress(addr: java.net.InetAddress): IPAddress =
        IPAddress(addr.getAddress)

    /**
     * Implicitly converts an <tt>IPAddress</tt> to a
     * <tt>java.net.InetAddress</tt>.
     *
     * @param ipAddr  the <tt>IPAddress</tt>
     *
     * @return the corresponding <tt>java.net.InetAddress</tt>
     */
    implicit def ipToInetAddress(ipAddr: IPAddress): java.net.InetAddress =
        java.net.InetAddress.getByAddress(ipAddr.address)
}

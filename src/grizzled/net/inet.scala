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
     * the address.
     *
     * @param addr  the 4-byte address
     *
     * @return the <tt>IPAddress</tt>
     */
    def apply(addr: Array[Byte]): IPAddress = new IPAddress(addr)

    /**
     * Create an <tt>IPAddress</tt>, given 1 to 4 integer arguments. If fewer
     * than 4 arguments are given, the missing arguments default to 0.
     *
     * @param addr  the bytes (as integers) of the address
     *
     * @return the <tt>IPAddress</tt>
     */
    def apply(addr: Int*): IPAddress =
    {
        require((addr.length > 0) && (addr.length <= 4))
        
        val addrList = (for (i <- addr) yield i) toList
        val addrBytes: Array[Int] = addrList match
        {
            case List(a, b, c, d) => Array(a, b, c, d)
            case List(a, b, c)    => Array(a, b, c, 0)
            case List(a, b)       => Array(a, b, 0, 0)
            case List(a)          => Array(a, 0, 0, 0)
            case _                => throw new AssertionError(addrList)
        }

        IPAddress(addrBytes)
    }

    /**
     * Create an <tt>IPAddress</tt>, given an 4-tuple of bytes representing
     * the address.
     *
     * @param addr  the 4-byte address
     *
     * @return the <tt>IPAddress</tt>
     */
    def apply(addr: Tuple4[Int, Int, Int, Int]): IPAddress =
        IPAddress(addr._1, addr._2, addr._3, addr._4)

    /**
     * Create an <tt>IPAddress</tt>, given an array of integers
     * representing the address. The address must be four single-byte
     * quantities, with each byte stored in an integer (for convenience of
     * use). Each integer is truncated to a byte before being used. Example
     * of use:
     *
     * <blockquote>
     * <pre>val ip = IPAddress(Array(192, 168, 1, 100))</pre>
     * </blockquote>
     *
     * @param addr  the 4-byte address
     *
     * @return the corresponding <tt>IPAddress</tt> object.
     */
    def apply(addr: Array[Int]): IPAddress = new IPAddress(addr map (_ toByte))

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

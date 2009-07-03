/*---------------------------------------------------------------------------*\
  This software is released under a BSD-style license:

  Copyright (c) 2009 Brian M. Clapper. All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

  1.  Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.

  2.  The end-user documentation included with the redistribution, if any,
      must include the following acknowlegement:

        "This product includes software developed by Brian M. Clapper
        (bmc@clapper.org, http://www.clapper.org/bmc/). That software is
        copyright (c) 2009 Brian M. Clapper."

      Alternately, this acknowlegement may appear in the software itself,
      if wherever such third-party acknowlegements normally appear.

  3.  Neither the names "clapper.org", "The Grizzled Scala Library",
      nor any of the names of the project contributors may be used to
      endorse or promote products derived from this software without prior
      written permission. For written permission, please contact
      bmc@clapper.org.

  4.  Products derived from this software may not be called "clapper.org
      Java Utility Library", nor may "clapper.org" appear in their names
      without prior written permission of Brian M. Clapper.

  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
  NO EVENT SHALL BRIAN M. CLAPPER BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
\*---------------------------------------------------------------------------*/

package grizzled.net

import java.net.InetAddress

/**
 * Represents an IP address. This class is similar to the
 * <tt>java.net.InetAddress</tt>, but it's designed to be more intuitive
 * and easier to use from Scala. This package provides implicit converters
 * to make this class compatible with <tt>java.net.InetAddress</tt>. The
 * converters ensure that all the (non-static) methods defined in the
 * <tt>java.net.InetAddress</tt> class are directly callable from an
 * instance of the Scala <tt>IPAddress</tt> class. For instance, the following
 * code is perfectly legal:
 *
 * <blockquote><pre>
 * val ip = IPAddress(192, 168, 2, 5)
 *
 * // Test if the address is reachable within 1 second.
 * println(ip + " is reachable? " + ip.isReachable(1000))
 *
 * // Get the canonical host name for (i.e., do a reverse lookup on) the
 * // address.
 * println(ip + " -> " + ip.getCanonicalHostName)
 *
 * // Determine whether it's the loopback address.
 * println(ip + " == loopback? " + ip.isLoopbackAddress)
 * </pre></blockquote>
 *
 * Here's an IPv6 example:
 *
 * <blockquote><pre>
 * val ip = IPAddress("fe80::21d:9ff:fea7:53e3")
 *
 * // Test if the address is reachable within 1 second.
 * println(ip + " is reachable? " + ip.isReachable(1000))
 *
 * // Get the canonical host name for (i.e., do a reverse lookup on) the
 * // address.
 * println(ip + " -> " + ip.getCanonicalHostName)
 *
 * // Determine whether it's the loopback address.
 * println(ip + " == loopback? " + ip.isLoopbackAddress)
 * </pre></blockquote>
 */
class IPAddress(val address: Array[Byte])
{
    require((address.length == 4) || (address.length == 16))

    /**
     * Return a printable version of this IP address.
     *
     * @return the printable version
     */
    override val toString: String =
    {
        val j: InetAddress = this
        j.getHostAddress
    }

    /**
     * Overloaded "==" method to test for equality.
     *
     * @param other  object against which to test this object
     *
     * @return <tt>true</tt> if equal, <tt>false</tt> if not
     */
    override def equals(other: Any): Boolean =
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
     * the address. The array must contain between 1 and 16 byte values.
     *
     * <ul>
     *   <li>If the array has fewer than four values, it is assumed to be
     *       an IPv4 address, and it will be padded with 0s to 4 bytes.
     *   <li>If the array has between four and 16 values, it is assumed to be
     *       an IPv6 address, and it will be padded with 0s to 16 bytes.
     *   <li>Anything else will cause an <tt>IllegalArgumentException</tt>
     *       to be thrown.
     * </ul>
     *
     * @param addr  the address
     *
     * @return the <tt>IPAddress</tt>
     */
    def apply(addr: Array[Byte]): IPAddress =
        IPAddress(addr toList)

    /**
     * Create an <tt>IPAddress</tt>, given an array of integers representing
     * the address. The array must contain between 1 and 16 integer values.
     * The integers will be truncated to 8-bit bytes.
     *
     * <ul>
     *   <li>If the array has fewer than four values, it is assumed to be
     *       an IPv4 address, and it will be padded with 0s to 4 bytes.
     *   <li>If the array has between four and 16 values, it is assumed to be
     *       an IPv6 address, and it will be padded with 0s to 16 bytes.
     *   <li>Anything else will cause an <tt>IllegalArgumentException</tt>
     *       to be thrown.
     * </ul>
     *
     * Example of use:
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
     * Create an <tt>IPAddress</tt>, given 1 to 16 integer arguments.
     * The integers will be truncated to 8-bit bytes.
     *
     * <ul>
     *   <li>If the argument list has fewer than four values, it is assumed
     *       to be an IPv4 address, and it will be padded with 0s to 4 bytes.
     *   <li>If the argument list has between four and 16 values, it is
     *       assumed to be an IPv6 address, and it will be padded with 0s to
     *       16 bytes.
     *   <li>Anything else will cause an <tt>IllegalArgumentException</tt>
     *       to be thrown.
     * </ul>
     *
     * Example of use:
     *
     * <blockquote>
     * <pre>val ip = IPAddress(Array(192, 168, 1, 100))</pre>
     * </blockquote>
     *
     * @param addr  the bytes (as integers) of the address
     *
     * @return the <tt>IPAddress</tt>
     */
    def apply(addr: Int*): IPAddress =
        IPAddress(addr map (_ toByte) toList)

    /**
     * Create an <tt>IPAddress</tt>, given a list of bytes representing the
     * address
     *
     * <ul>
     *   <li>If the list has fewer than four values, it is assumed to be an
     *       IPv4 address, and it will be padded with 0s to 4 bytes.
     *   <li>If the list has between four and 16 values, it is assumed to be
     *       an IPv6 address, and it will be padded with 0s to 16 bytes.
     *   <li>Anything else will cause an <tt>IllegalArgumentException</tt>
     *       to be thrown.
     * </ul>
     *
     * @param address  the list of address values
     *
     * @return the <tt>IPAddress</tt>
     */
    def apply(address: List[Byte]): IPAddress =
    {
        val zeroByte = 0 toByte

        val fullAddress: List[Byte] = address.length match
        {
            case 4 =>
                address

            case 16 =>
                address

            case 0 =>
                throw new IllegalArgumentException("Empty IP address.")

            case n if (n > 16) =>
                throw new IllegalArgumentException("IP address (" +
                                                   address.mkString(",") +
                                                   ") is too long.")

            case n =>
                val upper = if (n < 4) 4 else 16
                address ++ (for (i <- n until upper) yield zeroByte)
        }

        new IPAddress(fullAddress toArray)
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
        IPAddress(InetAddress.getByName(host).getAddress)

    /**
     * <p>Get a list of all <tt>IPAddress</tt> objects for a given host
     * name, based on whatever name service is configured for the running
     * system.</p>
     *
     * <p>The host name can either be a machine name, such as
     * "www.clapper.org", or a textual representation of its IP address. If
     * a literal IP address is supplied, only the validity of the address
     * format is checked.</p>
     *
     * <p>If the host is null, then this method return an <tt>IPAddress</tt>
     * representing an address of the loopback interfaced. See RFC 3330
     * section 2 and RFC 2373 section 2.5.3.</p>
     *
     * <p>This method corresponds to the <tt>getAllByName()</tt> method in
     * the <tt>java.net.InetAddress</tt> class.</p>
     *
     * @param hostname  the host name
     *
     * @return the list of matching <tt>IPAddress</tt> objects
     *
     * @throws UnknownHostException unknown host
     */
    def allForName(hostname: String): List[IPAddress] =
        InetAddress.getAllByName(hostname)
                   .map((x: InetAddress) => IPAddress(x.getAddress))
                   .toList

    /**
     * Implicitly converts a <tt>java.net.InetAddress</tt> to an
     * <tt>IPAddress</tt>.
     *
     * @param addr  the <tt>java.net.InetAddress</tt>
     *
     * @return the corresponding <tt>IPAddress</tt>
     */
    implicit def inetToIPAddress(addr: InetAddress): IPAddress =
        IPAddress(addr.getAddress)

    /**
     * Implicitly converts an <tt>IPAddress</tt> to a
     * <tt>java.net.InetAddress</tt>.
     *
     * @param ipAddr  the <tt>IPAddress</tt>
     *
     * @return the corresponding <tt>java.net.InetAddress</tt>
     */
    implicit def ipToInetAddress(ipAddr: IPAddress): InetAddress =
        InetAddress.getByAddress(ipAddr.address)
}

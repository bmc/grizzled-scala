package grizzled.net

import java.net.InetAddress

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

import grizzled.net.Implicits._

/** Represents an IP address. This class is similar to `java.net.InetAddress`,
  * but it's designed to be more intuitive and easier to use from Scala.
  * This package provides implicit converters to make this class compatible
  * with `java.net.InetAddress`. The converters ensure that all the
  * (non-static) methods defined in the `java.net.InetAddress` class are
  * directly callable from an instance of the Scala `IPAddress` class. For
  * instance, the following code is perfectly legal:
  *
  * {{{
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
  * }}}
  *
  * Here's an IPv6 example:
  *
  * {{{
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
  * }}}
  *
  * @param address the IPv4 or IPv6 address, as bytes
  */
class IPAddress(val address: Array[Byte]) {
  require((address.length == 4) || (address.length == 16))

  /** Convert the IP address to a number, suitable for numeric comparisons
    * against other IP addresses. The number is returned as a `BigInt` to
    * allow for both IPv4 and IPv6 addresses.
    *
    * @return the number
    */
  def toNumber: BigInt = {
    address.length match {
      case 4 /* IPv4 */ =>
        (((address(0) << 24) & 0xff000000) |
         ((address(1) << 16) & 0x00ff0000) |
         ((address(2) <<  8) & 0x0000ff00)  |
         (address(3)         & 0x000000ff)).
        toLong & 0x00000000ffffffffL

      case 16 /* IPv6 */ =>
        val j: InetAddress = this
        BigInt(j.getAddress)
    }
  }

  /** Convert this `IPAddress` to its corresponding `java.net.InetAddress`
    * object. Note that you can _also_ use the implicit conversions provided
    * by `grizzled.net.Implicits`.
    *
    * @return the corresponding `InetAddress`
    */
  def toInetAddress: InetAddress = InetAddress.getByAddress(this.address)

  /** Return a printable version of this IP address.
    *
    * @return the printable version
    */
  override val toString: String = {
    val j: InetAddress = this
    j.getHostAddress
  }

  /** Overloaded "==" method to test for equality.
    *
    * @param other  object against which to test this object
    *
    * @return `true` if equal, `false` if not
    */
  override def equals(other: Any): Boolean = {
    other match {
      case that: IPAddress =>
        that.address.toList == this.address.toList
      case _ =>
        false
    }
  }

  /** Overloaded hash method: Ensures that two `IPAddress` objects
    * that represent the same IP address have the same hash code.
    *
    * @return the hash code
    */
  override def hashCode: Int = address.toList.hashCode
}

/** Companion object to `IPAddress` class.
  */
object IPAddress {
  /** Singleton `IPAddress` for the local loop address.
    */
  val Localhost: IPAddress = new IPAddress(Array(127, 0, 0, 1))

  private val MaxIPv4 = BigInt("4294967295")

  // This is one butt-ugly regular expression. See
  // http://stackoverflow.com/a/17871737
  private val IPv6Pattern = """^(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))(%.*)?$""".r
  private val IPv4Pattern = """^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""".r

  /** Create an `IPAddress`, given an array of bytes representing
    * the address. The array must contain between 1 and 16 byte values.
    *
    * - If the array has fewer than four values, it is assumed to be
    *   an IPv4 address, and it will be padded with 0s to 4 bytes.
    * - If the array has between four and 16 values, it is assumed to be
    *   an IPv6 address, and it will be padded with 0s to 16 bytes.
    * - Anything else will cause an `IllegalArgumentException` to be thrown.
    *
    * @param addr  the address
    *
    * @return the `IPAddress` in a `Success`, or `Failure(exception)` on error.
    */
  def apply(addr: Array[Byte]): Try[IPAddress] = {
    IPAddress(addr.toList)
  }

  /** Create an `IPAddress`, given an array of integers representing
    * the address. The array must contain between 1 and 16 integer values.
    * The integers will be truncated to 8-bit bytes.
    *
    * - If the array has fewer than four values, it is assumed to be
    *   an IPv4 address, and it will be padded with 0s to 4 bytes.
    * - If the array has between four and 16 values, it is assumed to be
    *   an IPv6 address, and it will be padded with 0s to 16 bytes.
    * - Anything else will cause an `IllegalArgumentException` to be thrown.
    *
    * Example of use:
    *
    * {{{
    * val ip = IPAddress(Array(192, 168, 1, 100))
    * }}}
    *
    * @param addr  the address
    *
    * @return the `IPAddress` in a `Success`, or `Failure(exception)` on error.
    */
  def apply(addr: Array[Int]): Try[IPAddress] = {
    IPAddress(addr.map(_.toByte))
  }

  /** Create an `IPAddress`, given 1 to 16 integer arguments.
    * The integers will be truncated to 8-bit bytes.
    *
    * - If the array has fewer than four values, it is assumed to be
    *   an IPv4 address, and it will be padded with 0s to 4 bytes.
    * - If the array has between four and 16 values, it is assumed to be
    *   an IPv6 address, and it will be padded with 0s to 16 bytes.
    * - Anything else will cause an `IllegalArgumentException` to be thrown.
    *
    * Example of use:
    *
    * {{{
    * val ip = IPAddress(Array(192, 168, 1, 100))
    * }}}
    *
    * @param addr  the bytes (as integers) of the address
    *
    * @return the `IPAddress` in a `Success`, or `Failure(exception)` on error.
    */
  def apply(addr: Int*): Try[IPAddress] = {
    IPAddress(addr.map(_.toByte).toList)
  }

  /** Create an `IPAddress`, given a list of bytes representing the
    * address
    *
    * - If the array has fewer than four values, it is assumed to be
    *   an IPv4 address, and it will be padded with 0s to 4 bytes.
    * - If the array has between four and 16 values, it is assumed to be
    *   an IPv6 address, and it will be padded with 0s to 16 bytes.
    * - Anything else will cause an `IllegalArgumentException` to be thrown.
    * Example of use:
    *
    * {{{
    * val ip = IPAddress(List(192, 168, 1, 100))
    * }}}
    *
    * @param address  the list of address values
    *
    * @return the `IPAddress` in a `Success`, or `Failure(exception)` on error.
    */
  def apply(address: List[Byte]): Try[IPAddress] = {
    val zeroByte = 0.toByte

    val fullAddressRes = address.length match {
      case 4  => Success(address)
      case 16 => Success(address)
      case 0  => Failure(new IllegalArgumentException("Empty IP address."))

      case n if n > 16 =>
        Failure(new IllegalArgumentException(
          s"IP address ${address.mkString(".")} is too long."
        ))

      case n =>
        val upper = if (n < 4) 4 else 16
        Success(address ++ n.until(upper).map(_ => zeroByte))
    }

    fullAddressRes.map { bytes => new IPAddress(bytes.toArray) }
  }

  /** Create an `IPAddress` from a `java.net.InetAddress`. You can also
    * use the implicit conversions in `grizzled.net.Implicits`.
    *
    * @param inetAddress the `InetAddress`
    *
    * @return the `IPAddress`
    */
  def apply(inetAddress: InetAddress): IPAddress = {
    new IPAddress(inetAddress.getAddress)
  }

  /** Create an `IPAddress`, given a host name. Note that this function may
    *  do a DNS lookup if the host name is not an address string. To parse
    *  _just_ an address string, ignoring host names, use `parseAddress()`.
    *
    * @param host  the host name or address string
    *
    * @return the `IPAddress` in a `Success`, or `Failure(exception)` on error.
    */
  def apply(host: String): Try[IPAddress] = {
    Try { InetAddress.getByName(host).getAddress }.flatMap(IPAddress(_))
  }

  /** Create an `IPAddress` from a number.
    *
    * @param address the numeric IP address
    *
    * @return the `IPAddress` in a `Success`, or `Failure(exception)` on error.
    */
  def apply(address: BigInt): Try[IPAddress] = {
    if (address <= MaxIPv4) {
      // IPv4. Do it manually, because, sometimes, BigInt.toByteArray will
      // produce a byte array with one or more leading zero bytes, which
      // causes InetAddress.getByAddress() to throw an exception.
      val bytes = Array(
        ((address >> 24) & 0xff).toByte,
        ((address >> 16) & 0xff).toByte,
        ((address >> 8) & 0xff).toByte,
         (address & 0xff).toByte
      )
      IPAddress(bytes)
    }
    else {
      // IPv6.
      IPAddress(address.toByteArray)
    }
  }

  /** Parse an address string (IPv4 or IPv6) only. Host names will result
    * in an error return value. NOTE: This function strips any IPv6 interface
    * names, if they exist. That is, an address such as
    * "fe80::cab3:73ff:fe19:c600%en5" is parsed as "fe80::cab3:73ff:fe19:c600".
    *
    * @param addressString The address string
    *
    * @return the `IPAddress` in a `Success`, or `Failure(exception)` on error.
    */
  def parseAddress(addressString: String): Try[IPAddress] = {
    addressString match {
      case IPv4Pattern(_*)    => IPAddress(addressString)
      case IPv6Pattern(a, _*) => IPAddress(a)
      case _                  => Failure(new IllegalArgumentException(
                                   s"Invalid IP address: $addressString"
                                 ))
    }
  }

  /** Get a list of all `IPAddress` objects for a given host
    * name, based on whatever name service is configured for the running
    * system.
    *
    * The host name can either be a machine name, such as
    * "www.clapper.org", or a textual representation of its IP address. If
    * a literal IP address is supplied, only the validity of the address
    * format is checked.
    *
    * If the host is null, then this method return an `IPAddress`
    * representing an address of the loopback interfaced. See RFC 3330
    * section 2 and RFC 2373 section 2.5.3.
    *
    * This method corresponds to the `getAllByName()` method in
    * the `java.net.InetAddress` class.
    *
    * @param hostname  the host name
    *
    * @return a `Success` containing the list of resolved IP addresses, or
    *         `Failure(exception)` on error.
    */
  def allForName(hostname: String): Try[List[IPAddress]] = {
    val res = InetAddress.getAllByName(hostname)
                         .map((x: InetAddress) => IPAddress(x.getAddress))
                         .toList
    res.filter(_.isFailure) match {
      case failure :: rest => Try { List(failure.get) }
      case Nil             => Success(res.map(_.get))
    }
  }
}

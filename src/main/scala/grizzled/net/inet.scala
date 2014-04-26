/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2009, Brian M. Clapper
  All rights reserved.

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

package grizzled.net

import java.net.InetAddress

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

import grizzled.either.Implicits._
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
  override def equals(other: Any): Boolean =
    other match {
      case that: IPAddress =>
        that.address.toList == this.address.toList
      case _ =>
        false
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
  final val Localhost = IPAddress(Array(127, 0, 0, 1))

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
    * @return the `IPAddress` in a `Right`, on success; `Left(error)` on error.
    */
  def apply(addr: Array[Byte]): Either[String, IPAddress] = {
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
    * @return the corresponding `IPAddress` object.
    */
  def apply(addr: Array[Int]): Either[String, IPAddress] = {
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
    * @return the `IPAddress` in a `Right`, on success; `Left(error)` on error.
    */
  def apply(addr: Int*): Either[String, IPAddress] = {
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
    * @return the `IPAddress` in a `Right`, on success; `Left(error)` on error.
    */
  def apply(address: List[Byte]): Either[String, IPAddress] = {
    val zeroByte = 0.toByte

    val fullAddressRes = address.length match {
      case 4  => Right(address)
      case 16 => Right(address)
      case 0  => Left("Empty IP address.")

      case n if (n > 16) =>
        Left(s"IP address ${address.mkString(".")} is too long.")

      case n => {
        val upper = if (n < 4) 4 else 16
        Right(address ++ (n.until(upper).map(i => zeroByte)))
      }
    }

    fullAddressRes.map { bytes => new IPAddress(bytes.toArray) }
  }

  /** Create an `IPAddress`, given a host name.
    *
    * @param host  the host name
    *
    * @return the `IPAddress` in a `Right`, on success; `Left(error)` on error.
    */
  def apply(host: String): Either[String, IPAddress] = {
    IPAddress(InetAddress.getByName(host).getAddress)
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
    * @return a `Right` containing the list of resolved IP addresses, or
    *         `Left(error)` on error.
    */
  def allForName(hostname: String): Either[String, List[IPAddress]] = {
    val t = Try {
      InetAddress.getAllByName(hostname)
                 .map((x: InetAddress) => IPAddress(x.getAddress))
                 .toList
    }

    t match {
      case Failure(ex)   => Left(ex.getMessage)
      case Success(list) => {
        list.filter { _.isLeft }.map { _.left.get } match {
          case err :: errs => {
            Left((List(err) ++ errs).mkString(". "))
          }

          case Nil => {
            Right(list.filter { _.isRight }.map { _.right.get })
          }
        }
      }
    }
  }
}

package grizzled.net

import java.net.InetAddress
import scala.language.implicitConversions

/** Implicit conversions for network classes and types.
  */
object Implicits {

  /** Implicitly converts a `java.net.InetAddress` to an
    * `IPAddress`.
    *
    * @param addr  the `java.net.InetAddress`
    *
    * @return the `IPAddress`
    */
  implicit def inetToIPAddress(addr: InetAddress): IPAddress = {
    IPAddress(addr.getAddress).get
  }

  /** Implicitly converts an `IPAddress` to a
    * `java.net.InetAddress`.
    *
    * @param ipAddr  the `IPAddress`
    *
    * @return the corresponding `java.net.InetAddress`
    */
  implicit def ipToInetAddress(ipAddr: IPAddress): InetAddress = {
    InetAddress.getByAddress(ipAddr.address)
  }
}

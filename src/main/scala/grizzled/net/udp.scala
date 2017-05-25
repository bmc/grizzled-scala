package grizzled.net

import scala.annotation.tailrec

import java.net.{DatagramSocket => JDKDatagramSocket}
import java.net.{DatagramPacket => JDKDatagramPacket}
import grizzled.net.Implicits._
import scala.language.implicitConversions


/** A `UDPDatagramSocket` object represents a UDP datagram socket,
  * providing a simpler interface to sending and receiving UDP packets than
  * the one provided by the Java JDK.
  *
  * == Sending UDP Datagrams ==
  *
  * The easiest way to explain how to use this API is with some code. So,
  * without further ado, the following example shows how you might send the
  * string "foo" to port 2003 on host "foo.example.com".
  *
  * {{{
  * // Import the appropriate stuff.
  * import grizzled.net._
  *
  * // First, create an grizzled.net.IPAddress object for the destination
  * // machine.
  * val address = IPAddress("foo.example.com")
  *
  * // Next, create the socket object. Since we're sending the packet,
  * // we don't care what the local port is. By not specifying one, we allow
  * // the operating system to choose one one for us. Similarly, by not
  * // passing an explicit address, we indicate that the API should bind to a
  * // wildcard address, so that the packet can go out over any appropriate
  * // interface.
  * val socket = UDPDatagramSocket()
  *
  * // Next, use the sendString() method to send the string
  * socket.sendString("foo", address, 2003)
  *
  * // Finally, close the socket.
  * socket.close()
  * }}}
  *
  * That's pretty simple. However, using the utility methods provided by the
  * `UDPDatagramSocket` companion object, we can further simplify the
  * above code:
  *
  * {{{
  * // Import the appropriate stuff.
  * import grizzled.net._
  *
  * UDPDatagramSocket.sendString("foo", IPAddress("foo.example.com"), 2003)
  * }}}
  *
  * The `sendString()` method in the companion object takes care
  * of allocating the socket, sending the packet, and closing the socket.
  * Obviously, if you're planning on sending multiple packets at once, you'll
  * want to use the first example (perhaps in a loop), so you're not constantly
  * allocating and deallocating sockets. But sending a one-shot UDP packet
  * can be as simple as one line of code, as shown in the second example.
  *
  * Sending binary data is not much more complicated. You have to convert
  * the data to a stream of bytes, which the server must then decode. After
  * that, however, sending the bytes isn't much more difficult than sending
  * a string. Here's an example, which assumes that you have already encoded
  * the data to be send into an array of bytes.
  *
  * {{{
  * // Import the appropriate stuff.
  * import grizzled.net._
  *
  * // Encode the data into bytes. (Not shown.)
  * val data: Array[Byte] = encodeTheData()
  *
  * // Create the socket object.
  * val socket = UDPDatagramSocket()
  *
  * // Send the data.
  * socket.send(data, IPAddress("foo.example.com"), 2003)
  *
  * // Finally, close the socket.
  * socket.close()
  * }}}
  *
  * Once again, there's a simple utility method that does most of the work
  * for you:
  *
  * {{{
  * // Import the appropriate stuff.
  * import grizzled.net._
  *
  * // Encode the data into bytes. (Not shown.)
  * val data: Array[Byte] = encodeTheData()
  *
  * // Send the data.
  * UDPDatagramSocket.send(data, IPAddress("foo.example.com"), 2003)
  * }}}
  *
  * == Receiving UDP Datagrams ==
  *
  * When receiving UDP datagrams, you must first bind to an incoming
  * socket. That is, you must listen on the port to which clients are sending
  * their packets. In this example, we create the socket object with the
  * receiving port, and we allow the wildcard address to be used on the
  * local machine (permitting us to receive packets on any interface).
  *
  * {{{
  * // Import the appropriate stuff.
  * import grizzled.net._
  *
  * // Create the socket object.
  * val socket = UDPDatagramSocket(2003)
  * }}}
  *
  * Next, we want to receive and process the incoming data. Let's assume
  * that we're receiving the "foo" string (or, for that matter, any string)
  * being sent by the sample sender, above.
  *
  * {{{
  * // Receive and print strings.
  * while (true)
  *     println(socket.receiveString(1024))
  * }}}
  *
  * That code says, "Wait for incoming strings, using a 1024-byte buffer.
  * Then, decode the strings and print them to standard output.
  *
  * Receiving bytes isn't much more difficult.
  *
  * {{{
  * // Allocate a byte buffer. For efficiency, we'll re-use the same buffer
  * // on every incoming message.
  * val buf = Array.make[Byte](1024, 0)
  *
  * // Receive and process the incoming bytes. The process() method isn't
  * // shown.
  * while (true)
  * {
  *     val totalRead = socket.receive(buf)
  *     process(buf, totalRead)
  * }
  * }}}
  *
  * The above loop can be made even simpler (though a little more obscure),
  * since the `receive()` method is filling <i>our</i> buffer and
  * returning a count of the number of bytes it put into the buffer:
  *
  * {{{
  * while (true)
  *     process(buf, socket.receive(buf))
  * }}}
  */
trait UDPDatagramSocket {

  /** Whether or not the socket's broadcast flag is set. The broadcast
    * flag corresponds to the `SO_BROADCAST` socket option. When
    * this option is enabled, datagram sockets will receive packets sent
    * to a broadcast address, and they are permitted to send packets to a
    * broadcast address.
    *
    * @return whether or not the broadcast flag is set
    */
  def broadcast: Boolean

  /** Change the value of the socket's broadcast flag. The broadcast flag
    * corresponds to the `SO_BROADCAST` socket option. When this
    * option is enabled, datagram sockets will receive packets sent to a
    * broadcast address, and they are permitted to send packets to a
    * broadcast address.
    *
    * @param enable `true` to enable broadcast, `false` to disable it.
    */
  def broadcast_=(enable: Boolean)

  /** The local port to which the socket is bound.
    */
  def port: Int

  /** The local address to which the socket is bound.
    *
    * @return the address
    */
  def address: IPAddress

  /** Close the socket.
    */
  def close(): Unit

  /** Send data over the socket. Accepts any sequence of bytes (e.g.,
    * `Array[Byte]`, `List[Byte]`).
    *
    * @param data  the bytes to send
    */
  def send(data: Seq[Byte], address: IPAddress, port: Int): Unit

  /** Send string data over the socket. The internal UTF-16 strings are
    * converted to the specified encoding before being sent.
    *
    * @param data     the string to send
    * @param encoding the encoding to use
    * @param address  the IP address to receive the string
    * @param port     the destination port on the remote machine
    */
  def sendString(data: String,
                 encoding: String,
                 address: IPAddress,
                 port: Int): Unit = {
    import java.io.{ByteArrayOutputStream, OutputStreamWriter}
    import java.nio.charset.Charset

    val buf = new ByteArrayOutputStream()
    val writer = new OutputStreamWriter(buf, Charset.forName(encoding))
    writer.write(data)
    writer.flush()
    val bytes: Array[Byte] = buf.toByteArray
    send(bytes, address, port)
  }

  /** Send string data over the socket. Converts the string to UTF-8
    * bytes, then sends the bytes.
    *
    * @param data     the string to send
    * @param address  the IP address to receive the string
    * @param port     the destination port on the remote machine
    */
  def sendString(data: String, address: IPAddress, port: Int): Unit =
    sendString(data, "UTF-8", address, port)

  /** Receive a buffer of bytes from the socket. The buffer is dynamically
    * allocated and returned. For more efficiency, use the version of
    * `receive()` that takes a caller-supplied buffer.
    *
    * @param length  maximum number of bytes to receive
    *
    * @return a tuple containing the buffer of data (of size `length`)
    *         and the actual number of bytes read. The buffer might be longer
    *         than the number of bytes read.
    */
  def receive(length: Int): (Array[Byte], Int) = {
    val buf = makeByteArray(length)
    val total = receive(buf)

    (buf, total)
  }

  /** Receive a buffer of bytes from the socket, writing them into a
    * caller-supplied fixed-size buffer. One simple way to create such
    * a buffer:
    *
    * {{{
    * // Create a 512-byte buffer initialized to zeros.
    * val buf = Array.make[Byte](512, 0)
    * }}}
    *
    * @param buf  the buf into which to read the data
    *
    * @return actual number of bytes read
    */
  def receive(buf: Array[Byte]): Int

  /** Receive a string from the socket. The string is assumed to have
    * been encoded in the specified encoding and is decoded accordingly.
    *
    * @param length    maximum number of bytes (not characters) expected
    * @param encoding  the encoding to use when decoding the string
    *
    * @return the string
    */
  def receiveString(length: Int, encoding: String): String = {
    val buf = makeByteArray(length)
    receiveString(buf, encoding)
  }

  /** Receive a string from the socket. The string is assumed to have
    * been encoded in UTF-8 for transmission and is decoded accordingly.
    *
    * @param length  maximum number of bytes (not characters) expected
    *
    * @return the string
    */
  def receiveString(length: Int): String =
    receiveString(length, "UTF-8")

  /** Receive a string from the socket, using a caller-supplied receive
    * buffer to hold the bytes actually received over the wire. The string
    * is assumed to have been encoded in the specified encoding and is
    * decoded accordingly.
    *
    * @param buf  the buf into which to read the data
    * @param encoding  the encoding to use when decoding the string
    *
    * @return the string
    */
  def receiveString(buf: Array[Byte], encoding: String): String = {
    import java.io.{ByteArrayInputStream, InputStreamReader, StringWriter}
    import java.nio.charset.Charset

    val total = receive(buf)
    val bytes = (0 until total map (i => buf(i))).toArray
    val stream = new ByteArrayInputStream(bytes)
    val reader = new InputStreamReader(stream, Charset.forName(encoding))
    val chars = new StringWriter()

    @tailrec def readNextChar(): Unit = {
      val c: Int = reader.read()
      if (c != -1) {
        chars.write(c)
        readNextChar()
      }
    }

    readNextChar()
    chars.flush()
    chars.toString
  }

  /** Receive a string from the socket, using a caller-supplied receive
    * buffer to hold the bytes actually received over the wire. The string
    * is assumed to have been encoded in UTF-8 for transmission and is
    * decoded accordingly.
    *
    * @param buf  the buf into which to read the data
    *
    * @return the string
    */
  def receiveString(buf: Array[Byte]): String =
    receiveString(buf, "UTF-8")

  /** Make a byte array of a given length, initialized to zeros.
    *
    * @param length  length
    *
    * @return the array of bytes, initialized to zeros
    */
  private def makeByteArray(length: Int): Array[Byte] =
    (1 to length map(_ => 0.toByte)).toArray
}

/** Actual implementation of a UDP datagram socket. Implemented as a private
  * class that extends a trait primarily to hide the use of a JDK
  * `DatagramSocket` object.
  */
private class UDPDatagramSocketImpl(val socket: JDKDatagramSocket)
  extends UDPDatagramSocket {

  def broadcast: Boolean = socket.getBroadcast
  def broadcast_=(enable: Boolean) = socket.setBroadcast(enable)

  def port: Int = socket.getPort

  def address: IPAddress = socket.getInetAddress()

  def close(): Unit = socket.close()

  def send(data: Seq[Byte], address: IPAddress, port: Int): Unit = {
    val buf = data.toArray
    val packet = new JDKDatagramPacket(buf, buf.length, address, port)
    socket.send(packet)
  }

  def receive(buf: Array[Byte]): Int = {
    val packet = new JDKDatagramPacket(buf, buf.length)
    socket.receive(packet)
    packet.getLength
  }
}

/** Companion object for the `UDPDatagramSocket` trait, containing
  * methods to simplify creation of `UDPDatagramSocket` objects, as
  * well as some useful utility methods. See the documentation for the
  * `UDPDatagramSocket` trait for a full treatment on this API.
  *
  * @see UDPDatagramSocket
  */
object UDPDatagramSocket {
  /** Create a UDP datagram socket object that's bound to the specified
    * local port and IP address. The broadcast flag will initially be
    * clear.
    *
    * @param address  the local IP address
    * @param port     the local port
    *
    * @return the `UDPDatagramSocket` object
    */
  def apply(address: IPAddress, port: Int): UDPDatagramSocket =
    new UDPDatagramSocketImpl(new JDKDatagramSocket(port, address))

  /** Create a UDP datagram socket object that's bound to the specified
    * local port and the wildcard IP address. The broadcast flag will
    * initially be clear.
    *
    * @param port     the local port
    *
    * @return the `UDPDatagramSocket` object
    */
  def apply(port: Int): UDPDatagramSocket =
    new UDPDatagramSocketImpl(new JDKDatagramSocket(port))

  /** Create a UDP datagram socket object that's bound to any available
    * local port and the wildcard IP address. The broadcast flag will
    * initially be clear.
    *
    * @return the `UDPDatagramSocket` object
    */
  def apply(): UDPDatagramSocket =
    new UDPDatagramSocketImpl(new JDKDatagramSocket())

  /** Utility method for sending a non-broadcast UDP packet. This method
    * is equivalent to the following code snippet:
    *
    * {{{
    * // Bind to a local (source) UDP port.
    * val socket = UDPDatagramSocket.bind()
    * val address = IPAddress( /* details omitted */ )
    * val port: Int = ...
    *
    * // Send the bytes to the specified destination address and UDP port.
    * socket.send(bytes, address, port)
    *
    * // Close the socket
    * socket.close()
    * }}}
    *
    * @param bytes    sequence of bytes to send
    * @param address  IP address to which to send bytes
    * @param port     UDP port to which to send bytes
    */
  def send(bytes: Seq[Byte], address: IPAddress, port: Int): Unit =
    UDPDatagramSocket.send(bytes, address, port, false)

  /** Utility method for sending a UDP packet. This method is equivalent
    * to the following code snippet:
    *
    * {{{
    * // Bind to a local (source) UDP port.
    * val socket = UDPDatagramSocket()
    * val address = IPAddress( /* details omitted */ )
    * val port: Int = ...
    * val broadcast: Boolean = ...
    *
    * // Set (or clear) the broadcast flag.
    * socket.broadcast = broadcast
    *
    * // Send the bytes to the specified destination address and UDP port.
    * socket.send(bytes, address, port)
    *
    * // Close the socket
    * socket.close()
    * }}}
    *
    * @param bytes     sequence of bytes to send
    * @param address   IP address to which to send bytes
    * @param port      UDP port to which to send bytes
    * @param broadcast whether or not to enable broadcast on the socket
    */
  def send(bytes: Seq[Byte],
           address: IPAddress,
           port: Int,
           broadcast: Boolean): Unit = {
    // Bind a socket to a local (source) port.
    val socket = UDPDatagramSocket()

    // Set or clear the broadcast flag.
    socket.broadcast = broadcast

    // Send the encoded message to the broadcast address and destination
    // port.
    socket.send(bytes, address, port)

    // Close the socket.
    socket.close()
  }

  /** Utility method for sending a non-broadcast UDP packet consisting of
    * string data. The string data is encoded the specified encoding
    * before being sent. This method is equivalent to the following code
    * snippet:
    *
    * {{{
    * // Bind to a local (source) UDP port.
    * val socket = UDPDatagramSocket.bind()
    * val address = IPAddress( /* details omitted */ )
    * val port: Int = ...
    * val encoding: String = ...
    *
    * // Send the string to the specified destination address and UDP port.
    * socket.send(string, encoding, address, port)
    *
    * // Close the socket
    * socket.close()
    * }}}
    *
    * @param data     String data to send
    * @param encoding Encoding to use
    * @param address  IP address to which to send bytes
    * @param port     UDP port to which to send bytes
    */
  def sendString(data: String,
                 encoding: String,
                 address: IPAddress,
                 port: Int): Unit = {
    UDPDatagramSocket.sendString(data, encoding, address, port, broadcast=false)
  }

  /** Utility method for sending a non-broadcast UDP packet consisting of
    * string data. The string data is encoded in UTF-8 before being sent.
    * This method is equivalent to the following code snippet:
    *
    * {{{
    * // Bind to a local (source) UDP port.
    * val socket = UDPDatagramSocket.bind()
    * val address = IPAddress( /* details omitted */ )
    * val port: Int = ...
    *
    * // Send the string to the specified destination address and UDP port.
    * socket.send(string, address, port)
    *
    * // Close the socket
    * socket.close()
    * }}}
    *
    * @param data     String data to send
    * @param address  IP address to which to send bytes
    * @param port     UDP port to which to send bytes
    */
  def sendString(data: String, address: IPAddress, port: Int): Unit =
    UDPDatagramSocket.sendString(data, "UTF-8", address, port, broadcast=false)

  /** Utility method for sending a UDP packet consisting string data. The
    * string data is encoded in UTF-8 before being sent. This method is
    * equivalent to the following code snippet:
    *
    * {{{
    * // Bind to a local (source) UDP port.
    * val socket = UDPDatagramSocket()
    * val address = IPAddress( /* details omitted */ )
    * val port: Int = ...
    * val broadcast: Boolean = ...
    *
    * // Set (or clear) the broadcast flag.
    * socket.broadcast = broadcast
    *
    * // Send the string to the specified destination address and UDP port.
    * socket.send(string, address, port)
    *
    * // Close the socket
    * socket.close()
    * }}}
    *
    * @param data      String data to send
    * @param address   IP address to which to send bytes
    * @param port      UDP port to which to send bytes
    * @param broadcast Whether to enable broadcast or not.
    */
  def sendString(data: String,
                 address: IPAddress,
                 port: Int,
                 broadcast: Boolean): Unit = {
    UDPDatagramSocket.sendString(data, "UTF-8", address, port, broadcast)
  }

  /** Utility method for sending a UDP packet consisting string data. The
    * string data is encoded the specified encoding before being sent.
    * This method is equivalent to the following code snippet:
    *
    * {{{
    * // Bind to a local (source) UDP port.
    * val socket = UDPDatagramSocket()
    * val address = IPAddress( /* details omitted */ )
    * val port: Int = ...
    * val encoding: String = ...
    * val broadcast: Boolean = ...
    *
    * // Set (or clear) the broadcast flag.
    * socket.broadcast = broadcast
    *
    * // Send the string to the specified destination address and UDP port.
    * socket.send(string, encoding, address, port)
    *
    * // Close the socket
    * socket.close()
    * }}}
    *
    * @param data      String data to send
    * @param encoding  Encoding to use
    * @param address   IP address to which to send bytes
    * @param port      UDP port to which to send bytes
    * @param broadcast Whether to enable broadcast or not.
    */
  def sendString(data: String,
                 encoding: String,
                 address: IPAddress,
                 port: Int,
                 broadcast: Boolean): Unit = {
    // Bind a socket to a local (source) port.
    val socket = UDPDatagramSocket()

    // Set or clear the broadcast flag.
    socket.broadcast = broadcast

    // Send the encoded message to the broadcast address and destination
    // port.
    socket.sendString(data, encoding, address, port)

    // Close the socket.
    socket.close()
  }
}

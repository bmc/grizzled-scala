package grizzled.net

import java.net.{DatagramSocket => JDKDatagramSocket}
import java.net.{DatagramPacket => JDKDatagramPacket}
import java.net.InetAddress


/**
 * A <tt>UDPDatagramSocket</tt> object represents a UDP datagram socket,
 * providing a simpler interface to sending and receiving UDP packets than
 * the one provided by the Java JDK.
 */
trait UDPDatagramSocket
{
    /**
     * Whether or not the socket's broadcast flag is set. The broadcast
     * flag corresponds to the <tt>SO_BROADCAST</tt> socket option. When
     * this option is enabled, datagram sockets will receive packets sent
     * to a broadcast address, and they are permitted to send packets to a
     * broadcast address.
     *
     * @return whether or not the broadcast flag is set
     */
    def broadcast: Boolean

    /**
     * Change the value of the socket's broadcast flag. The broadcast flag
     * corresponds to the <tt>SO_BROADCAST</tt> socket option. When this
     * option is enabled, datagram sockets will receive packets sent to a
     * broadcast address, and they are permitted to send packets to a
     * broadcast address.
     *
     * @param enable <tt>true</tt> to enable broadcast, <tt>false</tt>
     *               to disable it.
     */
    def broadcast_=(enable: Boolean)

    /**
     * The local port to which the socket is bound.
     */
    def port: Int

    /**
     * The local address to which the socket is bound.
     */
    def address: IPAddress

    /**
     * Close the socket.
     */
    def close(): Unit

    /**
     * Send data over the socket. Accepts any sequence of bytes (e.g.,
     * <tt>Array[Byte]</tt>, <tt>List[Byte]</tt>).
     *
     * @param data  the bytes to send
     */
    def send(data: Seq[Byte], address: IPAddress, port: Int): Unit

    /**
     * Send string data over the socket. Converts the string to UTF-8
     * bytes, then sends the bytes.
     *
     * @param data  the bytes to send
     */
    def sendString(data: String, address: IPAddress, port: Int): Unit =
    {
        import java.io.{ByteArrayOutputStream, OutputStreamWriter}
        import java.nio.charset.Charset

        val buf = new ByteArrayOutputStream()
        val writer = new OutputStreamWriter(buf, Charset.forName("UTF-8"))
        writer.write(data)
        writer.flush()
        val bytes: Array[Byte] = buf.toByteArray
        send(bytes, address, port)
    }

    /**
     * Receive a buffer of bytes from the socket. The buffer is dynamically
     * allocated and returned. For more efficiency, use the version of
     * <tt>receive()</tt> that takes a caller-supplied buffer.
     *
     * @param length  maximum number of bytes to receive
     *
     * @return a tuple containing the buffer of data (of size <tt>length</tt>)
     *         and the actual number of bytes read. The buffer might be longer
     *         than the number of bytes read.
     */
    def receive(length: Int): (Array[Byte], Int) =
    {
        val buf = Array.make[Byte](length, 0)
        val total = receive(buf)

        (buf, total)
    }

    /**
     * Receive a buffer of bytes from the socket, writing them into a
     * caller-supplied fixed-size buffer. One simple way to create such
     * a buffer:
     *
     * <blockquote><pre>
     * // Create a 512-byte buffer initialized to zeros.
     * val buf = Array.make[Byte](512, 0)
     * </pre></blockquote>
     *
     * @param buf  the buf into which to read the data
     *
     * @return actual number of bytes read
     */
    def receive(buf: Array[Byte]): Int

    /**
     * Receive a string from the socket. The string is assumed to have
     * been encoded in UTF-8 for transmission and is decoded accordingly.
     *
     * @param length  maximum number of bytes (not characters) expected
     *
     * @return the string
     */
    def receiveString(length: Int): String =
    {
        val buf = Array.make[Byte](length, 0)
        receiveString(buf)
    }

    /**
     * Receive a string from the socket, using a caller-supplied receive
     * buffer to hold the bytes actually received over the wire. The string
     * is assumed to have been encoded in UTF-8 for transmission and is
     * decoded accordingly.
     *
     * @param buf  the buf into which to read the data
     *
     * @return the string
     */
    def receiveString(buf: Array[Byte]): String =
    {
        import java.io.{ByteArrayInputStream, InputStreamReader, StringWriter}
        import java.nio.charset.Charset
        import scala.collection.mutable.ArrayBuffer
        
        val total = receive(buf)
        val bytes = (for (i <- 0 until total) yield buf(i)).toArray
        val stream = new ByteArrayInputStream(bytes)
        val reader = new InputStreamReader(stream, Charset.forName("UTF-8"))
        val chars = new StringWriter()

        def readNextChar(): Unit =
        {
            val c: Int = reader.read()
            if (c != -1)
            {
                chars.write(c)
                readNextChar()
            }
        }

        readNextChar()
        chars.flush()
        println(chars.toString)
        chars.toString
    }
}

/**
 * Actual implementation of a UDP datagram socket. Implemented as a private
 * class that extends a trait primarily to hide the use of a JDK DatagramSocket
 * object.
 */
private class UDPDatagramSocketImpl(val socket: JDKDatagramSocket)
    extends UDPDatagramSocket
{
    def broadcast: Boolean = socket.getBroadcast()
    def broadcast_=(enable: Boolean) = socket.setBroadcast(enable)

    def port: Int = socket.getPort()
    def address: IPAddress = socket.getInetAddress()
    def close(): Unit = socket.close()

    def send(data: Seq[Byte], address: IPAddress, port: Int): Unit =
    {
        val buf = data toArray
        val packet = new JDKDatagramPacket(buf, buf.length, address, port)
        socket.send(packet)
    }

    def receive(buf: Array[Byte]): Int =
    {
        val packet = new JDKDatagramPacket(buf, buf.length)
        socket.receive(packet)
        packet.getLength
    }
}

object UDPDatagramSocket
{
    /**
     * Create a UDP datagram socket object that's bound to the specified
     * local port and IP address. The broadcast flag will initially be
     * clear.
     *
     * @param address  the local IP address
     * @param port     the local port
     *
     * @return the <tt>UDPDatagramSocket</tt> object
     */
    def apply(address: IPAddress, port: Int): UDPDatagramSocket =
        new UDPDatagramSocketImpl(new JDKDatagramSocket(port, address))

    /**
     * Create a UDP datagram socket object that's bound to the specified
     * local port and the wildcard IP address. The broadcast flag will
     * initially be clear.
     *
     * @param port     the local port
     *
     * @return the <tt>UDPDatagramSocket</tt> object
     */
    def apply(port: Int): UDPDatagramSocket =
        new UDPDatagramSocketImpl(new JDKDatagramSocket(port))

    /**
     * Create a UDP datagram socket object that's bound to any available
     * local port and the wildcard IP address. The broadcast flag will
     * initially be clear.
     *
     * @param port     the local port
     *
     * @return the <tt>UDPDatagramSocket</tt> object
     */
    def apply(): UDPDatagramSocket =
        new UDPDatagramSocketImpl(new JDKDatagramSocket())

    /**
     * Utility method for sending a non-broadcast UDP packet. This method
     * is equivalent to the following code snippet:
     *
     * <blockquote><pre>
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
     * </pre></blockquote>
     *
     * @param bytes    sequence of bytes to send
     * @param address  IP address to which to send bytes
     * @param port     UDP port to which to send bytes
     */
    def send(bytes: Seq[Byte], address: IPAddress, port: Int): Unit =
        UDPDatagramSocket.send(bytes, address, port, false)

    /**
     * Utility method for sending a UDP packet. This method is equivalent
     * to the following code snippet:
     *
     * <blockquote><pre>
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
     * </pre></blockquote>
     *
     * @param bytes     sequence of bytes to send
     * @param address   IP address to which to send bytes
     * @param port      UDP port to which to send bytes
     * @param broadcast whether or not to enable broadcast on the socket
     */
    def send(bytes: Seq[Byte], 
             address: IPAddress, 
             port: Int,
             broadcast: Boolean): Unit =
    {
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

    /**
     * Utility method for sending a non-broadcast UDP packet consisting of
     * string data. The string data is encoded in UTF-8 before being sent.
     * This method is equivalent to the following code snippet:
     *
     * <blockquote><pre>
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
     * </pre></blockquote>
     *
     * @param data     String data to send
     * @param address  IP address to which to send bytes
     * @param port     UDP port to which to send bytes
     */
    def sendString(data: String, address: IPAddress, port: Int): Unit =
        UDPDatagramSocket.sendString(data, address, port, false)

    /**
     * Utility method for sending a UDP packet consisting string data. The
     * string data is encoded in UTF-8 before being sent. This method is
     * equivalent to the following code snippet:
     *
     * <blockquote><pre>
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
     * </pre></blockquote>
     *
     * @param data     String data to send
     * @param address  IP address to which to send bytes
     * @param port     UDP port to which to send bytes
     */
    def sendString(data: String,
                   address: IPAddress, 
                   port: Int,
                   broadcast: Boolean): Unit =

    {
        // Bind a socket to a local (source) port.
        val socket = UDPDatagramSocket()

        // Set or clear the broadcast flag.
        socket.broadcast = broadcast

        // Send the encoded message to the broadcast address and destination
        // port.
        socket.sendString(data, address, port)

        // Close the socket.
        socket.close()
    }
}

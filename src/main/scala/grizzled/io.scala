/**
 * I/O-related classes and utilities. This package is distinguished from
 * the <tt>grizzled.file</tt> package in that this package operates on
 * already-open Java <tt>InputStream<tt>, <tt>OutputStream</tt>,
 * <tt>Reader</tt> and <tt>Writer</tt> objects, and on Scala
 * <tt>Source</tt> objects.
 */
package grizzled.io

import java.io.{InputStream,
                OutputStream,
                Reader,
                Writer}

/**
 * Provides additional methods, over and above those already present in
 * the Java <tt>InputStream</tt> class. The <tt>implicits</tt> object
 * contains implicit conversions between <tt>RichInputStream</tt> and
 * <tt>InputStream</tt>.
 *
 * @param inputStream  the input stream to wrap
 */
class RichInputStream(val inputStream: InputStream)
{
    /**
     * Copy the input stream to an output stream, stopping on EOF. This
     * method does no buffering. If you want buffering, make sure you use a
     * <tt>java.io.BufferedInputStream</tt> and a
     * <tt>java.io.BufferedOutputStream</tt>. This method does not close
     * either stream.
     *
     * @param out  the output stream
     */
    def copyTo(out: OutputStream): Unit =
    {
        val c: Int = inputStream.read()
        if (c != -1)
        {
            out.write(c)
            // Tail recursion means never having to use a var.
            copyTo(out)
        }
    }
}

/**
 * Implicit conversions between <tt>RichInputStream</tt> and
 * <tt>InputStream</tt> objects.
 */
object implicits
{
    implicit def inputStreamToRichInputStream(inputStream: InputStream) =
        new RichInputStream(inputStream)

    implicit def richInputStreamInputStream(richInputStream: RichInputStream) =
        richInputStream.inputStream
}

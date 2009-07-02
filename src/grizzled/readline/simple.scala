/**
 * Simple, pure Scala implementation of the traits defined in the base
 * readline package.
 */
package grizzled.readline.simple

import grizzled.readline._

/**
 * Simple history implementation.
 */
private[simple] class SimpleHistory extends History
{
    import scala.collection.mutable.ArrayBuffer

    private val history = new ArrayBuffer[String]

    /**
     * Add a line to the history.
     *
     * @param line  the line to add
     */
    def +=(line: String) = history += line

    /**
     * Get the contents of the history buffer, in a list.
     *
     * @return the history lines
     */
    def get: List[String] = history.toList

    /**
     * Clear the history buffer
     */
    def clear = history.clear
}

/**
 * Simple implementation of the Readline trait.
 */
private[readline] class SimpleImpl(appName: String,
                                   val autoAddHistory: Boolean)
    extends Readline
{
    import java.io.{InputStreamReader, LineNumberReader}

    val name = this.getClass.getName
    val history = new SimpleHistory
    val input = new LineNumberReader(new InputStreamReader(System.in))

    private[readline] def doReadline(prompt: String): Option[String] =
    {
        try
        {
            print(prompt)
            val s = input.readLine
            if (s == null)
                None
            else
                Some(s)
        }

        catch
        {
            case e: java.io.EOFException => None
        }
    }
}

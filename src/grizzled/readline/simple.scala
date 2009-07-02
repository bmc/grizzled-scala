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

    protected def append(line: String) = history += line

    def get: List[String] = history.toList

    def clear = history.clear

    def last: Option[String] =
    {
        history.length match
        {
            case 0 => None
            case _ => Some(history.last)
        }
    }
}

/**
 * Simple implementation of the Readline trait.
 */
private[readline] class SimpleImpl(appName: String,
                                   override val autoAddHistory: Boolean)
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

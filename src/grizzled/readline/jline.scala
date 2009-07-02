/**
 * JLine implementation of the traits defined in the base readline package.
 */
package grizzled.readline.jline

import grizzled.readline._
import _root_.jline.{Completor => JLineCompleter, ConsoleReader}

/**
 * History implementation that wraps the JLine history API.
 */
private[jline] class JLineHistory(val reader: ConsoleReader)
    extends History
{
    val history = reader.getHistory

    protected def append(line: String) = history.addToHistory(line)

    def get: List[String] =
    {
        import scala.collection.mutable.ArrayBuffer

        val result = new ArrayBuffer[String]
        val it = history.getHistoryList.iterator
        while (it.hasNext)
            result += it.next.asInstanceOf[String]
        result.toList
    }

    def clear = history.clear

    def last: Option[String] =
    {
        val s = history.current
        if ((s == null) || (s.length == 0)) 
            None 
        else
            Some(s)
    }
}

/**
 * JLine implementation of the Readline trait.
 */
private[readline] class JLineImpl(appName: String,
                                  override val autoAddHistory: Boolean)
    extends Readline
{
    val name = this.getClass.getName
    val reader = new ConsoleReader
    val history = new JLineHistory(reader)
    reader.addCompletor(jlCompleter)
    reader.setUseHistory(false) // we'll do it manually
    val self = this

    // Need to use a Scala existential type as the parameter to the
    // complete() method, below, because Scala will type the java.util.List
    // parameter as List[_], and will then complain when we try to add a
    // String to the list. The existential type gets around that problem by
    // supplying (by force) type information for the list. See Chapter 29
    // (section 29.3) in the "Programming in Scala" book.
    type JList = java.util.List[T] forSome {type T}
    type JSList = java.util.List[String]

    object jlCompleter extends JLineCompleter
    {
        def complete(buf: String, cursor: Int, completions: JList): Int =
        {
            def save(scalaCompletions: List[String], javaCompletions: JSList) =
            {
                // Hiding this in a method, and casting the incoming
                // java.util.List parameter, keeps Scala's type
                // checker from bitching.
                for (s <- scalaCompletions)
                    javaCompletions.add(s)
            }

            val line = if (buf == null) "" else buf
            // Find the start of the token.
            val i = if (buf == null) 0 else buf.lastIndexOf(' ')
            val tokenStart = if (i >= 0) (i + 1) else 0
            val token = buf.substring(tokenStart)

            val matches = self.completer.complete(token, line)
            save(matches, completions.asInstanceOf[JSList])
            if (completions.size == 0) -1 else tokenStart
        }
    }

    private[readline] def doReadline(prompt: String): Option[String] =
    {
        try
        {
            print(prompt)
            val s = reader.readLine
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

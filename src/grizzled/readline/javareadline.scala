/**
 * JavaReadline implementation of the traits defined in the base readline
 * package. Handles both GNU Readline and Editline.
 */
package grizzled.readline.javareadline

import grizzled.readline._
import grizzled.collection._
import grizzled.collection.implicits._
import org.gnu.readline.{Readline => JavaReadline,
                         ReadlineCompleter => JavaReadlineCompleter,
                         ReadlineLibrary => JavaReadlineLibrary}

/**
 * History implementation that wraps the JavaReadline history API.
 */
private[javareadline] class ReadlineHistory extends History
{
    /**
     * Add a line to the history.
     *
     * @param line  the line to add
     */
    def +=(line: String) = JavaReadline.addToHistory(line)

    /**
     * Get the contents of the history buffer, in a list.
     *
     * @return the history lines
     */
    def get: List[String] =
    {
        import _root_.java.util.ArrayList

        val history = new ArrayList[String]

        JavaReadline.getHistory(history)

        val result = {for (line <- history) yield line}
        result.toList
    }

    /**
     * Clear the history buffer
     */
    def clear = JavaReadline.clearHistory
}

/**
 * JavaReadline implementation of the Readline trait.
 */
private[readline] class JavaReadlineImpl(appName: String,
                                         val autoAddHistory: Boolean,
                                         library: JavaReadlineLibrary)
    extends Readline
{
    val name = this.getClass.getName
    val history = new ReadlineHistory
    val self = this

    JavaReadline.load(library)
    JavaReadline.initReadline(appName)
    JavaReadline.setCompleter(rlCompleter)
    Runtime.getRuntime.addShutdownHook(
        new Thread
        {
            override def run = JavaReadline.cleanup
        }
    )

    object rlCompleter extends JavaReadlineCompleter
    {
        private var iterator: Iterator[String] = null

        def completer(text: String, state: Int): String =
        {
            if (state == 0)
            {
                // First call to completer. Get list of matches.

                val matches = self.completer.complete(text, text)
                iterator = matches.elements
            }

            if (iterator.hasNext)
            {
                val next = iterator.next
                if (next.startsWith(text))
                    next
                else
                    null
            }
            else
                null
        }
    }

    private[readline] def doReadline(prompt: String): Option[String] =
    {
        try
        {
            val s = JavaReadline.readline(prompt, /* add to history */ false)
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

/**
 * JavaReadline implementation of the Readline trait, specialized for the
 * EditLine library.
 */
private[readline] class EditlineImpl(appName: String,
                                     autoAddHistory: Boolean)
    extends JavaReadlineImpl(appName,
                             autoAddHistory,
                             JavaReadlineLibrary.Editline)

/**
 * JavaReadline implementation of the Readline trait, specialized for the
 * GNU Readline library.
 */
private[readline] class GNUReadlineImpl(appName: String,
                                        autoAddHistory: Boolean)
    extends JavaReadlineImpl(appName,
                             autoAddHistory,
                             JavaReadlineLibrary.GnuReadline)

/**
 * JavaReadline implementation of the Readline trait, specialized for the
 * Getline library.
 */
private[readline] class GetlineImpl(appName: String,
                                        autoAddHistory: Boolean)
    extends JavaReadlineImpl(appName,
                             autoAddHistory,
                             JavaReadlineLibrary.Getline)

// Compile this one before running it.

import grizzled.readline._
import grizzled.readline.Readline.ReadlineType._
import grizzled.readline.Readline.ReadlineType

import java.io.File

/**
 * Simple tester.
 */
object TestReadline
{
    val types = Map("readline" -> ReadlineType.GNUReadline,
                    "editline" -> ReadlineType.EditLine,
                    "getline"  -> ReadlineType.GetLine,
                    "jline"    -> ReadlineType.JLine,
                    "simple"   -> ReadlineType.Simple)

    def main(args: Array[String]) =
    {
        val t =
            if (args.length == 0)
                ReadlineType.GNUReadline
            else
                types(args(0))

        val r = Readline(t, "Test", false)
        println("Using: " + r)

        val HistoryPath = "/tmp/readline.hist"
        val historyFile = new File(HistoryPath)
        if (historyFile.exists)
        {
            println("Loading history file \"" + HistoryPath + "\"")
            r.history.load(HistoryPath)
        }

        object completer extends Completer
        {
            val completions = List("linux", "lisa", "mac", "freebsd", "freedos")

            def complete(token: String, line: String): List[String] =
            {
                println("token=\"" + token + "\", line=\"" + line + "\"")

                {for (c <- completions; 
                      if (c.startsWith(token))) yield c}.toList

            }
        }

        r.completer = completer
        var line = r.readline("? ")
        while (line != None)
        {
            val s = line.get
            if (s != "")
            {
                if ((s == "history") || (s == "h"))
                {
                    for ((s, i) <- r.history.get.zipWithIndex)
                        println(i + ": " + s)
                }

                else
                {
                    r.history += s
                    println(s)
                }
            }

            line = r.readline("? ")
        }

        println("Saving history to file \"" + HistoryPath + "\"")
        r.history.save(HistoryPath)
    }
}

// Run this as a script.

import grizzled.cmd._
import grizzled.string.implicits._

object Foo extends CommandHandler
{
    val name = "foo"
    private def validArgs = List("bar", "baz", "fred")
    override val aliases = List("fool")
    val help = """Does that foo thang"""
    def runCommand(commandName: String, unparsedArgs: String): Unit = 
        println("*** " + commandName + " " + unparsedArgs)
    override def complete(token: String, line: String): List[String] =
        if (token == "")
            validArgs
        else
            validArgs.filter(_ startsWith token)
}

class Prompt(val cmd: Test) extends CommandHandler
{
    val name = "prompt"
    val help = """Change the prompt. Usage: prompt string"""

    // Test the "more input needed" capability by insisting that this
    // command end with a period. If the string doesn't end with a period,
    // the command loop should keep prompting (using the secondary prompt)
    // until it does.
    override def moreInputNeeded(line: String) = (! line.rtrim.endsWith("."))

    // Handle the command by changing the prompt.
    def runCommand(commandName: String, unparsedArgs: String): Unit = 
    {
        if (unparsedArgs.length > 1)
	    cmd.prompt = unparsedArgs.substring(0, unparsedArgs.length -1)
    }
}

class Test extends CommandInterpreter("Test")
{
    val HistoryPath = "/tmp/test.hist"
    val handlers = List(Foo, 
                        new Prompt(this), 
                        new HistoryHandler(this),
                        new RedoHandler(this))
    var prompt = super.primaryPrompt
    override def primaryPrompt = prompt

    override def preLoop =
    {
        println("Using " + readline + " readline implementation.")
        val historyFile = new java.io.File(HistoryPath)
        if (historyFile.exists && historyFile.isFile)
        {
            println("Loading history file \"" + HistoryPath + "\"")
            history.load(historyFile.getPath)
        }
    }

    override def postLoop =
    {
        println("Saving history to file \"" + HistoryPath + "\"")
        history.save(HistoryPath)
    }

    override def preCommand(line: String) =
        if (line.ltrim.startsWith("#"))
            ""
        else
            line
}

new Test().mainLoop

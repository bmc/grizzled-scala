// Run this as a script.

import grizzled.cmd._
import grizzled.readline.{ListCompleter, CompletionToken}
import grizzled.string.implicits._

object Foo extends CommandHandler
{
    val CommandName = "foo"
    private def validArgs = List("bar", "baz", "fred")
    private val completer = new ListCompleter(validArgs)
    override val aliases = List("fool")
    val Help = """Does that foo thang"""
    def runCommand(commandName: String, unparsedArgs: String): CommandAction = 
    {
        println("*** " + commandName + " " + unparsedArgs)
        KeepGoing
    }

    override def complete(token: String,
                          allTokens: List[CompletionToken],
                          line: String): List[String] =
        completer.complete(token, allTokens, line)
}

class CommentHandler extends HiddenCommandHandler
{
    val CommandName = "rem"

    def runCommand(commandName: String, unparsedArgs: String): CommandAction =
    {
        println("*** Comment. Ignored.")
        KeepGoing
    }
}

class BlockHandler extends BlockCommandHandler
{
    val CommandName = "{"
    val EndCommand = """\}""".r

    val Help = "{}"

    def runCommand(commandName: String, unparsedArgs: String): CommandAction = 
    {
        println("*** commandName: " + commandName + ", args=" + unparsedArgs)
        KeepGoing
    }
}

class Prompt(val cmd: Test) extends CommandHandler
{
    val CommandName = "prompt"
    val Help = """Change the prompt. Usage: prompt string"""

    // Test the "more input needed" capability by insisting that this
    // command end with a period. If the string doesn't end with a period,
    // the command loop should keep prompting (using the secondary prompt)
    // until it does.
    override def moreInputNeeded(line: String) = (! line.rtrim.endsWith("."))

    // Handle the command by changing the prompt.
    def runCommand(commandName: String, unparsedArgs: String): CommandAction = 
    {
        if (unparsedArgs.length > 1)
	    cmd.prompt = unparsedArgs.substring(0, unparsedArgs.length -1)
        KeepGoing
    }
}

object ExitHandler extends CommandHandler
{
    val CommandName = "exit"
    val Help = "Exit the interpreter"

    def runCommand(commandName: String, unparsedArgs: String): CommandAction = 
        Stop
}

class Test extends CommandInterpreter("Test")
{
    val HistoryPath = "/tmp/test.hist"
    val handlers = List(Foo, 
                        new Prompt(this), 
                        new HistoryHandler(this),
                        new RedoHandler(this),
                        new BlockHandler,
                        new CommentHandler,
                        ExitHandler)
    var prompt = super.primaryPrompt
    override def primaryPrompt = prompt

    override def StartCommandIdentifier = super.StartCommandIdentifier + "-"

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

    override def handleEOF =
    {
        error("Use Ctrl-C or type \"exit\" to exit.")
        KeepGoing
    }

    override def postLoop =
    {
        println("Saving history to file \"" + HistoryPath + "\"")
        history.save(HistoryPath)
    }

    override def preCommand(line: String) =
        if (line.ltrim.startsWith("#"))
            Some("")
        else
            Some(line)
}

new Test().mainLoop

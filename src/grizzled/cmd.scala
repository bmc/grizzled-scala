/**
 * Classes and objects to aid in the construction of line-oriented command
 * interpreters. This package is very similar, in concept, to the Python
 * <tt>cmd</tt> module (though its implementation differs quite a bit).
 */
package grizzled.cmd

import grizzled.readline.Readline.ReadlineType._
import grizzled.readline.Readline.ReadlineType
import grizzled.readline.Readline
import grizzled.string.implicits._

import java.io.EOFException

/**
 * Trait for an object (or class) that handles a single command. All logic
 * for a given command is embodied in a single object that mixes in this
 * trait.
 */
trait CommandHandler
{
    /**
     * The name of the command. This name, or any of the aliases (see below)
     * will cause the command to be invoked.
     */
    val name: String

    /**
     * Additional aliases for the command, if any.
     */
    val aliases: List[String] = Nil

    private[this] def allNames = name :: aliases

    /**
     * Compares a command name (that the user typed in, for instance) to
     * this command's name. The default implementation of this method simply
     * forces both names to lower case before comparing them. Overridden
     * definitions of this method can apply other matching criteria.
     *
     * @param candidate  the candidate name to be compared with this one
     *
     * @return <tt>true</tt> if they match, <tt>false</tt> if not
     */
    def matches(candidate: String): Boolean =
        allNames.filter(_.toLowerCase == candidate.toLowerCase) match
        {
            case Nil => false
            case _   => true
        }

    /**
     * Compares a prefix string to this command name, to determine whether
     * the prefix string could possibly be completed by this command's name.
     * This method is obviously used to facilitate tab-completion. The default
     * implementation of this method simply forces both strings to lower case
     * before performing a substring comparison between them. Overridden
     * definitions of this method can apply other matching criteria.
     *
     * @param prefix  the prefix to compare
     *
     * @return <tt>true</tt> if this command's name could be a valid completion
     *         of <tt>prefix</tt>, <tt>false</tt> if not
     */
    def couldComplete(prefix: String): Boolean = 
        allNames.filter(_.toLowerCase startsWith prefix.toLowerCase) match
        {
            case Nil => false
            case _   => true
        }

    /**
     * This method is called after a line is read that matches this command,
     * to determine whether more lines need to be read to finish the command.
     * The default implementation returns <tt>false</tt>, meaning a single
     * input line suffices for the entire command. Implementing classes or
     * objects can override this method to ensure that the command has a
     * required terminating character (e.g., a ";"), doesn't end with a line
     * continuation character (e.g., "\"), or whatever the syntax requires.
     */
    def moreInputNeeded(lineSoFar: String): Boolean = false

    /**
     * The help for this command.
     */
    val help: String

    /**
     * Handle the command. The first white space-delimited token in the command
     * string is guaranteed to match the name of this command, by the rules of
     * the <tt>matches()</tt> method.
     *
     * @param command      the command that invoked this handler
     * @param unparsedArgs the remainder of the unparsed command line
     */
    def handle(command: String, unparsedArgs: String): Unit

    /**
     * Perform completion on the command, returning the possible completions.
     *
     * @param commandLine  the entire command line so far
     * @param token        the token within the command line to complete
     * @param tokenStart   the starting index of the token within the line
     * @param tokenEnd     the ending index of the token within the line
     *
     * @return the list of completions for <tt>token</tt>, or <tt>Nil</tt>
     */
    def complete(commandLine: String,
                 token:       String,
                 tokenStart:  Int,
                 tokenEnd:    Int): List[String] = Nil
}

/**
 * Base class of any interpreter.
 */
abstract class CommandInterpreter(val appName: String,
                                  readlineCandidates: List[ReadlineType])
{
    /**
     * The readline implementation being used.
     */
    val readline = findReadline(readlineCandidates)

    if (readline == null)
        throw new Exception("Unable to load a readline library.")

    /**
     * Alternate constructor taking a single readline implementation. Fails
     * if that readline implementation cannot be found.
     *
     * @param appName   application name
     * @param readline  readline implementation
     */
    def this(appName: String, readline: ReadlineType) = 
        this(appName, List(readline))

    /**
     * Alternate constructor that tries all known readline implementations,
     * in this order:
     *
     * <ul>
     *   <li> GNU Readline
     *   <li> Editline
     *   <li> Getline
     *   <li> JLine
     *   <li> Simple (pure Java)
     * </ul>
     *
     * @param appName   application name
     */
    def this(appName: String) = 
        this(appName, List(ReadlineType.GNUReadline,
                           ReadlineType.EditLine,
                           ReadlineType.GetLine,
                           ReadlineType.JLine,
                           ReadlineType.Simple))

    /**
     * The primary prompt string.
     */
    def primaryPrompt = "? "

    /**
     * The second prompt string, used when additional input is being
     * retrieved.
     */
    def secondaryPrompt = "> "

    /**
     * List of handlers. The subclass must define this value to contain a
     * list of its handlers. The <tt>allHandlers</tt> property will combine
     * this list with the help handler to get the list of all handlers.
     * If you define your own help handler, you'll have to override the
     * <tt>helpHandler</tt> property to return your help handler, instead of
     * the default one.
     */
    val handlers: List[CommandHandler]

    /**
     * Get all handlers. By default, this property combines the
     * <tt>handlers</tt> value with the default help handler,
     * <tt>HelpHandler</tt>. If you define your own help handler, you'll
     * have to override the <tt>helpHandler</tt> property to return your
     * help handler, instead of the default one.
     */
    final def allHandlers: List[CommandHandler] = helpHandler :: handlers

    /**
     * Get the help handler. Override this property if you want to supply
     * your own help handler.
     */
    def helpHandler = HelpHandler

    /**
     * Default handler for help messages. To redefine how help is generated,
     * create your own handler and redefine the <tt>helpHandler</tt>
     * property to return it.
     */
    object HelpHandler extends CommandHandler
    {
        val name = "help"
        override val aliases = List("?")
        override val help = """This message"""

        private lazy val nameSorter = (a: String, b: String) => a < b

        private def helpHelp =
        {
            // Help only.

            val commandNames = handlers.map(_.name).sort(nameSorter)
            println("Help is available for the following commands:")
            for (c <- commandNames) 
                println(c)
        }

        private def helpCommand(names: List[String])
        {
            for (name <- names)
            {
                findCommand(name) match
                {
                    case Some(cmd) =>
                        val header = "Help for \"" + cmd.name + "\""
                        println("\n" + header + "\n" + ("-" * header.length))
                        if (cmd.aliases != Nil)
                            println("Aliases: " + cmd.aliases.mkString(", "))
                        println(cmd.help)

                    case None =>
                        println("\nHelp is unavailable for \"" + name + "\"")
                }
            }
        }

        def handle(command: String, unparsedArgs: String): Unit =
        {
            if (unparsedArgs == "")
                helpHelp
            else
                helpCommand(CmdUtil.tokenize(unparsedArgs))
        }
    }

    /**
     * Called just before the main loop (<tt>mainLoop()</tt>) begins its
     * command loop, this hook method can be used for initialization. The
     * default implementation does nothing.
     */
    def preLoop: Unit = return

    /**
     * Called immediately after the main loop (<tt>mainLoop()</tt>) ends
     * its command loop, this hook method can be used for cleanup. The
     * default implementation does nothing.
     */
    def postLoop: Unit = return

    /**
     * Called just before a command line is interpreted, this hook method can
     * be used to modify the command.
     *
     * @param commandLine  the unparsed command line
     *
     * @return the modified line. If a null or an empty string is returned,
     *         the command is skipped. (This can be useful for handling
     *         commands, for instance.)
     */
    def preCommand(commandLine: String): String = commandLine

    /**
     * Repeatedly issue a prompt, accept input, parse an initial prefix from
     * the received input, and dispatch to execution handlers.
     */
    final def mainLoop: Unit =
    {
        def splitCommandAndArgs(line: String): (String, String) =
        {
            // Strip the command name.
            val lTrimmed = line.ltrim
            val firstBlank = lTrimmed.indexOf(' ')

            if (firstBlank == -1)
                (lTrimmed, "")
            else
                (lTrimmed.substring(0, firstBlank).trim,
                 lTrimmed.substring(firstBlank).ltrim)

        }

        def process(line: String)
        {
            val (commandName, unparsedArgs) = splitCommandAndArgs(line)

            findCommand(commandName) match
            {
                case None    => 
                    println("*** Unknown command: " + commandName)
                case Some(c) => 
                    if (c.moreInputNeeded(line))
                        readAndProcess(line, secondaryPrompt)
                    else
                        c.handle(commandName, unparsedArgs)
            }
        }

        def readAndProcess(prefix: String, prompt: String): Unit =
        {
            readline.readline(prompt) match
            {
                case None       => 
                    // Use an exception to indicate EOF, to unwind the stack.
                    throw new EOFException

                case Some(line) => 
                    val line2 = preCommand(prefix + line)
                    if ((line2 != null) && (line2.trim.length > 0))
                        process(line2)
            }

            readAndProcess("", primaryPrompt)
        }

        preLoop
        try
        {
            readAndProcess("", primaryPrompt)
        }

        catch
        {
            case e: EOFException =>
        }

        finally
        {
            postLoop
        }
    }

    /**
     * Given a list of readline implementation types, attempt to load them
     * one by one, stopping at the one that actually loads. See the
     * <tt>grizzled.readline</tt> package.
     *
     * @param libs  list of readline libraries to try
     *
     * @return the one that was found, or null if none were found.
     */
    private[this] def findReadline(libs: List[ReadlineType]): Readline =
    {
        if (libs == Nil)
            null

        else
        {
            val lib = libs.head
            val result = 
                try
                {
                    Readline(lib, appName, /* autoAddHistory */ false)
                }

                catch
                {
                    case e: UnsatisfiedLinkError => findReadline(libs.tail)
                }

            result
        }
    }

    /**
     * Given a command line, find the command handler that matches,
     * checking the name against the command handler name and its aliases.
     *
     * @param name  the command name
     *
     * @return An option containing the command handler, or <tt>None</tt>
     *         for no match
     */
    private[this] def findCommand(name: String): Option[CommandHandler] =
    {
        val command = allHandlers filter (_ matches name)
        assert(command.length < 2)
        command match
        {
            case Nil       => None
            case List(cmd) => Some(cmd)
        }
    }
}

/**
 * Miscellaneous useful utility functions.
 */
object CmdUtil
{
    def tokenize(commandLine: String): List[String] =
        commandLine.trim.split("[ \t]+").toList
}

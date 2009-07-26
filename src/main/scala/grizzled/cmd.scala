/*
  ---------------------------------------------------------------------------
  This software is released under a BSD-style license:

  Copyright (c) 2009 Brian M. Clapper. All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

  1.  Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.

  2.  The end-user documentation included with the redistribution, if any,
      must include the following acknowlegement:

        "This product includes software developed by Brian M. Clapper
        (bmc@clapper.org, http://www.clapper.org/bmc/). That software is
        copyright (c) 2009 Brian M. Clapper."

      Alternately, this acknowlegement may appear in the software itself,
      if wherever such third-party acknowlegements normally appear.

  3.  Neither the names "clapper.org", "The Grizzled Scala Library",
      nor any of the names of the project contributors may be used to
      endorse or promote products derived from this software without prior
      written permission. For written permission, please contact
      bmc@clapper.org.

  4.  Products derived from this software may not be called "clapper.org
      Java Utility Library", nor may "clapper.org" appear in their names
      without prior written permission of Brian M. Clapper.

  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
  NO EVENT SHALL BRIAN M. CLAPPER BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ---------------------------------------------------------------------------
*/

/**
 * Classes and objects to aid in the construction of line-oriented command
 * interpreters. This package is very similar, in concept, to the Python
 * <tt>cmd</tt> module (though its implementation differs quite a bit).
 */
package grizzled.cmd

import grizzled.readline.Readline.ReadlineType._
import grizzled.readline.Readline.ReadlineType
import grizzled.readline.{Readline, Completer, History}

import grizzled.GrizzledString._

/**
 * Actions returned by handlers.
 */
sealed abstract class CommandAction
case object KeepGoing extends CommandAction
case object Stop extends CommandAction

/**
 * Mapped commands.
 */
sealed abstract class Command
case object EOFCommand extends Command
case object EmptyCommand extends Command
case class KnownCommand(handler: CommandHandler, 
                        fullInputLine: String,
                        name: String,
                        args: String) extends Command
case class UnknownCommand(fullInputLine: String,
                          name: String, 
                          args: String) extends Command

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
    val CommandName: String

    /**
     * Additional aliases for the command, if any.
     */
    val aliases: List[String] = Nil

    /**
     * The help for this command. The help string is written as is to the
     * screen. It is not wrapped, indented, or otherwise reformatted. It
     * may be a single string or a multiline string.
     */
    val Help: String

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
     * Compares a prefix string to this command name and its aliases, to
     * determine whether the prefix string could possibly be completed by
     * the name or aliases. This method is obviously used to facilitate
     * tab-completion. The default implementation of this method simply
     * forces both strings to lower case before performing a substring
     * comparison between them. Overridden definitions of this method can
     * apply other matching criteria.
     *
     * @param prefix  the prefix to compare
     *
     * @return a list of the strings (name and/or aliases) that could be
     *         completed by <tt>prefix</tt>, or <tt>Nil</tt>.
     */
    def commandNameCompletions(prefix: String): List[String] =
        allNames.filter(_.toLowerCase startsWith prefix.toLowerCase)

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
     * Handle the command. The first white space-delimited token in the command
     * string is guaranteed to match the name of this command, by the rules of
     * the <tt>matches()</tt> method.
     *
     * @param command      the command that invoked this handler
     * @param unparsedArgs the remainder of the unparsed command line
     *
     * @return <tt>KeepGoing</tt> to tell the main loop to continue,
     *         or <tt>Stop</tt> to tell the main loop to be done.
     */
    def runCommand(command: String, unparsedArgs: String): CommandAction

    /**
     * Perform completion on the command, returning the possible completions.
     *
     * @param token        the token within the command line to complete
     * @param commandLine  the entire command line so far
     *
     * @return the list of completions for <tt>token</tt>, or <tt>Nil</tt>
     */
    def complete(token: String, commandLine: String): List[String] = Nil

    /**
     * Convenience method to retrieve the combined list of names and aliases.
     */
    private[this] def allNames = CommandName :: aliases
}

/**
 * Base class for command interpreters.
 * 
 * <p><tt>CommandInterpreter</tt> is the base class of any command
 * interpreter.</p> This class and the <tt>CommandHandler</tt> trait
 * provide a simple framework for writing line-oriented
 * command-interpreters. This framework is conceptually similar to the
 * Python <tt>cmd</tt> module and its <tt>Cmd</tt> class, though the
 * implementation differs substantially in places.</p>
 *
 * <p>For reading input from the console, <tt>CommandInterpreter</tt> will
 * use of any of the readline libraries supported by the
 * <tt>grizzled.readline</tt> package. All of those libraries support a
 * persistent command history, and most support command completion and
 * command editing.</p>
 *
 * <p>A command line consists of an initial command name, followed by a list
 * of arguments to that command. The <tt>CommandInterpreter</tt> class's
 * command reader automatically separates the command and the remaining
 * arguments, via the <tt>splitCommandAndArgs()</tt> method. Parsing the
 * arguments is left to the actual command implementation. The rules for how
 * the command is split from the remainder of the input line are outlined
 * in the documentation for the <tt>splitCommandAndArgs()</tt> method.</p>
 *
 * @param appName             the application name, used by some readline
 *                            libraries for key-binding
 * @param readlineCandidates  list of readline libraries to try to load, in
 *                            order. The <tt>ReadlineType</tt> values are
 *                            defined by the <tt>grizzled.readline</tt>
 *                            package.
 * @param useAnsiColors       <tt>true</tt> to use ANSI terminal colors in
 *                            some output, <tt>false</tt> to avoid them.
 */
abstract class CommandInterpreter(val appName: String,
                                  readlineCandidates: List[ReadlineType])
{
    /**
     * Assumed output width of the screen.
     */
    val OutputWidth = 79

    /**
     * For sorting names.
     */
    private lazy val NameSorter = (a: String, b: String) => a < b

    /**
     * Default list of readline libraries to try, in order.
     */
    val DefaultReadlineLibraryList = List(ReadlineType.GNUReadline,
                                          ReadlineType.EditLine,
                                          ReadlineType.GetLine,
                                          ReadlineType.JLine,
                                          ReadlineType.Simple)

    /**
     * The readline implementation being used.
     */
    val readline = readlineCandidates match
    {
        case Nil => findReadline(DefaultReadlineLibraryList)
        case _   => findReadline(readlineCandidates)
    }

    if (readline == null)
        throw new Exception("Unable to load a readline library.")

    readline.completer = CommandCompleter

    /**
     * Get the history object being used to record command history.
     *
     * @return the <tt>grizzled.readline.History</tt> object
     */
    val history: History = readline.history

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
        this(appName, Nil)

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
     * <tt>StartCommandIdentifier</tt> is the list of characters that are
     * permitted as the first character of a white space-delimited,
     * multicharacter command name. All other characters are assumed to
     * introduce single-character commands. The default value permits
     * alphanumeric initial characters. Subclasses may override this value
     * to permit additional, or different, starting characters for
     * multicharacter command names. See the <tt>splitCommandAndArgs()</tt>
     * method for more details.
     */
    def StartCommandIdentifier = "abcdefghijklmnopqrstuvwxyz" +
                                 "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                                 "0123456789"

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
        val CommandName = "help"
        override val aliases = List("?")
        override val Help = """This message"""

        private def helpHelp =
        {
            import scala.collection.mutable.ArrayBuffer

            // Help only.

            val commandNames = sortedCommandNames(false)

            println("Help is available for the following commands:")
            println("-" * OutputWidth)

            print(columnarize(commandNames, OutputWidth))
        }

        private def helpCommand(names: List[String])
        {
            for (name <- names)
            {
                findCommand(name) match
                {
                    case Some(cmd) =>
                        val header = "Help for \"" + cmd.CommandName + "\""
                        println("\n" + header + "\n" + ("-" * header.length))
                        if (cmd.aliases != Nil)
                            printf("Aliases: %s\n\n", 
                                   cmd.aliases.mkString(", "))

                        println(cmd.Help)

                    case None =>
                        println("\nHelp is unavailable for \"" + name + "\"")
                }
            }
        }

        def runCommand(command: String, unparsedArgs: String): CommandAction =
        {
            if (unparsedArgs == "")
                helpHelp
            else
                helpCommand(CmdUtil.tokenize(unparsedArgs))

            KeepGoing
        }
    }

    /**
     * Readline completion handler. Conditionally completes command names
     * or defers to command handlers for individual completion.
     */
    private object CommandCompleter extends Completer
    {
        def complete(token: String, line: String): List[String] =
            unsortedCompletions(token, line).sort(NameSorter)

        private def unsortedCompletions(token: String, 
                                        line: String): List[String] =
        {
            if (line == "")
            {
                // Tab completion at the beginning of the line. Return a
                // list of all commands.

                sortedCommandNames(true)
            }

            else
            {
                val (commandName, unparsedArgs) = splitCommandAndArgs(line)

                if (unparsedArgs == "")
                {
                    if (token == "")
                    {
                        // Command is complete, but there are no arguments,
                        // and the user pressed TAB after the completed
                        // command. Treat this as completion for a given
                        // command.
                        completeForCommand(commandName, token, line)
                    }

                    else
                    {
                        // Treat this as completion of a command name.

                        matchingCommandsFor(token)
                    }
                }

                else
                {
                    // Completion for a specific command. Find the handler
                    // and let it do the work.
                    completeForCommand(commandName, token, line)
                }
            }
        }

        private def matchingCommandsFor(token: String): List[String] =
        {
            val completions =
            {
                for {handler <- allHandlers
                     val completions = handler.commandNameCompletions(token)
                     if (completions != Nil)}
                yield completions
            }.toList

            completions.flatten[String]
        }

        private def completeForCommand(commandName: String,
                                       token:       String,
                                       line:        String): List[String] =
        {
            findCommand(commandName) match
            {
                case None          => Nil
                case Some(handler) => handler.complete(token, line)
            }
        }
    }

    /**
     * Emit an error message in a consistent way. May be overridden by
     * subclasses. The default implementation prints errors in red.
     *
     * @param message the message to emit
     */
    def error(message: String) =
        println(Console.RED + "Error: " + message + Console.RESET)

    /**
     * Emit a warning message in a consistent way. May be overridden by
     * subclasses. The default implementation prints the message with the
     * prefix "Warning: ".
     *
     * @param message the message to emit
     */
    def warning(message: String) = 
        println(Console.YELLOW + "Warning: " + message + Console.RESET)

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
     * edit the command.
     *
     * @param commandLine  the command line
     *
     * @return The possibly edited command, Some("") to signal an empty
     *         command, or None to signal EOF. 
     */
    def preCommand(commandLine: String): Option[String] = Some(commandLine)

    /**
     * Called after a command line is interpreted. The default implementation
     * simply returns <tt>KeepGoing</tt>.
     *
     * @param command      the command that invoked this handler
     * @param unparsedArgs the remainder of the unparsed command line
     *
     * @return <tt>KeepGoing</tt> to tell the main loop to continue,
     *         or <tt>Stop</tt> to tell the main loop to be done.
     */
    def postCommand(command: String, unparsedArgs: String): CommandAction =
        return KeepGoing

    /**
     * Called when an empty command line is entered in response to the
     * prompt. The default version of this method simply returns
     * <tt>KeepGoing</tt>.
     *
     * @return <tt>KeepGoing</tt> to tell the main loop to continue,
     *         or <tt>Stop</tt> to tell the main loop to be done.
     */
    def handleEmptyCommand: CommandAction = return KeepGoing

    /**
     * Called when an end-of-file condition is encountered while reading a
     * command (On Unix-like systems, with some readline libraries, this
     * happens when the user pressed Ctrl-D). prompt. The default version
     * of this method simply returns <tt>Stop</tt>, causing the command
     * loop to exit.
     *
     * @return <tt>KeepGoing</tt> to tell the main loop to continue,
     *         or <tt>Stop</tt> to tell the main loop to be done.
     */
    def handleEOF: CommandAction = return Stop

    /**
     * Called when a command is entered that isn't recognized. The default
     * version of this method prints an error message and returns
     * <tt>KeepGoing</tt>.
     *
     * @param commandName  the command name
     * @param unparsedArgs the command arguments
     *
     * @return <tt>KeepGoing</tt> to tell the main loop to continue,
     *         or <tt>Stop</tt> to tell the main loop to be done.
     */
    def handleUnknownCommand(commandName: String, 
                             unparsedArgs: String): CommandAction =
    {
        error("Unknown command: " + commandName)
        KeepGoing
    }

    /**
     * Called when an exception occurs during the main loop. This method
     * can handle the exception however it wants; it must return either
     * <tt>KeepGoing</tt> or <tt>Stop</tt>. The default version
     * of this method dumps the exception stack trace and returns
     * <tt>Stop</tt>.
     *
     * @param e  the exception
     *
     * @return <tt>KeepGoing</tt> to tell the main loop to continue,
     *         or <tt>Stop</tt> to tell the main loop to be done.
     */
    def handleException(e: Exception): CommandAction =
    {
        error("Exception: " + e.getClass.toString)
        e.printStackTrace(System.out)
        Stop
    }

    /**
     * Split a command from its argument list, returning the command as
     * one string and the remaining unparsed argument string as the other
     * string. The commmand name is parsed from the remaining arguments
     * using the following rules:
     *
     * <ul>
     *   <li> If the first non-white character if the input line is in the
     *        <tt>StartCommandIdentifier</tt> string, then the command is
     *        assumed to be a identifier that is separated from the arguments
     *        by white space.
     *   <li> If the first character if the input line is not in the
     *        <tt>StartCommandIdentifier</tt> string, then the command is
     *        assumed to be a single-character command, with the arguments
     *        immediately following the single character.
     * </ul>
     *
     * <p>The <tt>StartCommandIdentifier</tt> string is an overridable field
     * defined by this class, consisting of the characters permitted to start
     * a multicharacter command. By default, it consists of alphanumerics.
     * Subclasses may override it to permit additional, or different, starting
     * characters for multicharacter commands.</p>
     *
     * <p>For example, using the default identifier characters, this function
     * will break the following commands into command + arguments as shown:</p>
     *
     * <table border="0" cellpadding="2">
     *   <tr valign="top">
     *     <td><tt>foo bar baz</tt></td>
     *     <td>Command <tt>foo</tt>, argument string <tt>"bar baz"</tt></td>
     *   </tr>
     *
     *   <tr valign="top">
     *     <td><tt>!bar</tt></td>
     *     <td>Command <tt>!</tt> argument string <tt>"bar baz"</tt></td>
     *   </tr>
     *
     *   <tr valign="top">
     *     <td><tt>? one two</tt></td>
     *     <td>Command <tt>?</tt> argument string <tt>"one two"</tt></td>
     *   </tr>
     * </table>
     *
     * <p>Subclasses may override this method to parse commands differently.</p>
     *
     * @param line  the input type
     *
     * @return A (<i>commandName</i>, <i>argumentString</i>) 2-tuple
     */
    def splitCommandAndArgs(line: String): (String, String) =
    {
        // Strip the command name.
        val lTrimmed = line.ltrim

        if (lTrimmed == "")
            ("", "")

        else if (StartCommandIdentifier.indexOf(lTrimmed(0)) == -1)
            // Single character command.
            (lTrimmed(0).toString, (lTrimmed drop 1).ltrim)

        else
        {
            // White space-delimited identifier command. Parse accordingly.

            val firstBlank = lTrimmed.indexOf(' ')

            if (firstBlank == -1)
                (lTrimmed, "")

            else
                (lTrimmed.substring(0, firstBlank).trim,
                 lTrimmed.substring(firstBlank).ltrim)
        }
    }

    /**
     * Handles a command line, just as if it had been typed directly at the
     * prompt.
     *
     * @param commandLine  the command line
     *
     * @return <tt>KeepGoing</tt> to tell the main loop to continue,
     *         or <tt>Stop</tt> to tell the main loop to be done.
     */
    final def handleCommand(commandLine: Option[String]): CommandAction =
    {
        try
        {
            val commandLine2 = commandLine match
            {
                case None       => None
                case Some(line) => preCommand(line)
            }

            mapCommandLine(commandLine2) match
            {
                case EOFCommand  => 
                    handleEOF

                case EmptyCommand =>
                    handleEmptyCommand

                case UnknownCommand(commandLine, commandName, args) =>
                    history += commandLine
                    handleUnknownCommand(commandName, args)

                case KnownCommand(handler, commandLine, commandName, args) =>
                    history += commandLine
                    val action = handler.runCommand(commandName, args)
                    if (action != Stop)
                        postCommand(commandName, args)
                    action
            }
        }

        catch
        {
            case e: Exception =>
                handleException(e)
        }
    }

    /**
     * Take a list of strings and print them in columns.
     *
     * @param strings  the list of strings
     * @param width    how wide a virtual screen to use
     *
     * @return a possibly multiline string containing the columnar output
     */
    def columnarize(strings: List[String], width: Int): String =
    {
        import scala.collection.mutable.ArrayBuffer
        import grizzled.math.util.max

        val buf = new ArrayBuffer[Char] 

        // Lay them out in columns. Simple-minded for now.
        val colSize = max(strings.map(_.length): _*) + 2
        val colsPerLine = width / colSize
        for ((s, i) <- strings.zipWithIndex)
        {
            if ((i % colsPerLine) == 0)
                buf += '\n'

            val padding = " " * (colSize - s.length)
            buf ++= (s + padding)
        }

        buf += '\n'
        buf mkString ""
    }

    /**
     * Repeatedly issue a prompt, accept input, parse an initial prefix from
     * the received input, and dispatch to execution handlers.
     */
    final def mainLoop: Unit =
    {
        preLoop
        try
        {
            readAndProcessCommand
        }

        finally
        {
            postLoop
        }
    }

    /**
     * Read a command line
     *
     * @return <tt>Some(line)</tt> if a line was read, or <tt>None</tt> for
     *         EOF. Note that an empty string results in <tt>Some("")</tt>,
     *         not <tt>None</tt>.
     */
    private def readCommand: Option[String] =
    {
        def doRead(lineSoFar: String, usePrompt: String): Option[String] =
        {
            val s = readline.readline(usePrompt)
            val readlineResult = s match
            {
                case None if (lineSoFar == "") => None
                case None                      => Some(lineSoFar)
                case Some(line)                => Some(lineSoFar + line)
            }

            readlineResult match
            {
                case None =>
                    None

                case Some(line) if ((line == null) || (line.trim == "")) =>
                    Some("")

                case Some(line) =>
                    val (name, args) = splitCommandAndArgs(line)

                    findCommand(name) match
                    {
                        case None =>
                            Some(line)

                        case Some(handler) =>
                            if (! handler.moreInputNeeded(line))
                                Some(line)
                            else
                                doRead(line + " ", secondaryPrompt)
                    }
            }
        }

        doRead("", primaryPrompt)
    }

    /**
     * Map a command string to a <tt>Command</tt> object.
     *
     * @param commandLine  the command line
     *
     * @return the <tt>Command</tt> object
     */
    private def mapCommandLine(commandLine: Option[String]): Command =
    {
        commandLine match
        {
            case None =>
                EOFCommand

            case Some(line) if ((line == null) || (line.trim == "")) =>
                EmptyCommand

            case Some(line) =>
                val (commandName, args) = splitCommandAndArgs(line)
                findCommand(commandName) match
                {
                    case None =>
                        UnknownCommand(line, commandName, args)
                    case Some(handler) =>
                        KnownCommand(handler, line, commandName, args)
                }
        }
    }

    /**
     * Read and process the next command.
     *
     * @param prompt  Prompt to issue
     */
    private def readAndProcessCommand: Unit =
    {
        if (handleCommand(readCommand) == KeepGoing)
            readAndProcessCommand
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
            try
            {
                Readline(lib, appName, /* autoAddHistory */ false)
            }

            catch
            {
                case e: UnsatisfiedLinkError => findReadline(libs.tail)
            }
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

    /**
     * Get a list of all command names, sorted.
     *
     * @param includeAliases whether or not to include aliases
     *
     * @return All command names, sorted.
     */
    private def sortedCommandNames(includeAliases: Boolean): List[String] =
    {
        val namesOnly = allHandlers.map(_.CommandName)
        val allNames = 
            if (includeAliases)
                // Extract the aliases, producing a list of lists of strings.
                // Then, flatten that list of lists into a single list of
                // strings.
                namesOnly ::: allHandlers.map(_.aliases).flatten[String]
            else
                namesOnly

        allNames.sort(NameSorter)
    }
}

/**
 * Miscellaneous useful utility functions.
 */
object CmdUtil
{
    def tokenize(unparsedArgs: String): List[String] =
    {
        val trimmed = unparsedArgs.trim
        if (trimmed == "")
            Nil
        else
            trimmed.split("[ \t]+").toList
    }
}

/**
 * Simple history command handler.
 * 
 * <p>A simple "history" (alias: "h") handler that displays the history to
 * standard output. This history handler supports the following usage:</p>
 *
 * <blockquote><pre>history [n]</pre></blockquote>
 *
 * <p>Where <i>n</i> is the number of (most recent) history entries to show.
 * If absent, <i>n</i> defaults to the size of the history buffer.</p>
 *
 * <p>This handler is <i>not</i> installed by default. It is provided as a
 * convenience, for command interpreters to use if desired.</p>
 */
class HistoryHandler(val cmd: CommandInterpreter) extends CommandHandler
{
    val CommandName = "history"
    override val aliases = List("h")

    val history = cmd.history 
    val Help = """Show history. 
                 |
                 |Usage: history [n]
                 |
                 |where n is the number of recent history entries to show.""".
               stripMargin

    def runCommand(commandName: String, unparsedArgs: String): CommandAction = 
    {
        val tokens = CmdUtil.tokenize(unparsedArgs)
        val historyCommands = history.get

        def show(commands: List[String], startingNumber: Int) =
            for ((line, i) <- commands.zipWithIndex)
                format("%3d: %s\n", startingNumber + i, line)

        if (tokens.length > 0)
        {
            try
            {
                val n = tokens(0).toInt
                if (historyCommands.length > n)
                    show(historyCommands.slice(historyCommands.length -n,
                                               historyCommands.length), 
                         historyCommands.length - n)
                else
                    show(historyCommands, 1)
            }

            catch
            {
                case e: NumberFormatException =>
                    println("Bad number: \"" + tokens(0) + "\"")
                    Nil
            }
        }

        else
        {
            show(historyCommands, 1)
        }

        KeepGoing
    }
}

/**
 * Simple "redo command" handler.
 * 
 * <p>A simple "redo" command handler that supports re-issuing a numbered
 * command from the history or the last command with a given prefix. For
 * example, it supports the following syntaxes:</p>
 *
 * <table border="0" cellpadding="2">
 *   <tr valign="top">
 *     <td><tt>!10</tt>, <tt>! 10</tt>, or <tt>r 10</tt></td>
 *     <td>Repeat the 10th command in the history.</td>
 *   </tr>
 *
 *   <tr valign="top">
 *     <td><tt>!ec</tt>, <tt>! ec</tt>, or <tt>r ec</tt></td>
 *     <td>Repeat the most recent command whose name begins with "ec"</td>
 *   </tr>
 *
 *   <tr valign="top">
 *     <td><tt>!!</tt>, <tt>! !</tt>, <tt>r</tt></td>
 *     <td>Repeat the last command.</td>
 *   </tr>
 * </table>
 *
 * <p>This handler is <i>not</i> installed by default. It is provided as a
 * convenience, for command interpreters to use if desired.</p>
 */
class RedoHandler(val cmd: CommandInterpreter) extends CommandHandler
{
    val CommandName = "r"
    override val aliases = List("!", "/")

    val history = cmd.history 
    val Help = """Reissue a command, by partial name or number.
                 |
                 |Usage: r namePrefix  -or-  !namePrefix
                 |       r number      -or-  !number
                 |
                 | For example:
                 |
                 | !ec   Run the most recent command whose name starts with "ec"
                 | r ec  Run the most recent command whose name starts with "ec"
                 | !10   Run the command numbered 10 in the history
                 | r 10  Run the command numbered 10 in the history""".
               stripMargin

    def runCommand(commandName: String, unparsedArgs: String): CommandAction = 
    {
        val lTrimmedArgs = unparsedArgs.ltrim
        val historyBuf = history.get

        if ( ((commandName == "r") && (lTrimmedArgs == "")) ||
             ((commandName == "!") && (lTrimmedArgs == "!")) )
        {
            // Reissue last command from history.

            if (historyBuf.length == 0)
            {
                cmd.error("Empty command history.")
                KeepGoing
            }
            else
            {
                val command = historyBuf(historyBuf.length - 1)
                cmd.handleCommand(Some(command))
            }
        }

        else
        {
            assert(lTrimmedArgs != "")

            val commandId = lTrimmedArgs.split("[ \t]+")(0)

            try
            {
                val n = commandId.toInt
                if ( (n < 1) || (n > historyBuf.length) )
                {
                    cmd.error("No command has number " + n)
                    KeepGoing
                }
                else
                {
                    val command = historyBuf(n - 1)
                    cmd.handleCommand(Some(command))
                }
            }

            catch
            {
                case _: NumberFormatException => 
                    rerunCommandByPrefix(commandId)
            }
        }
    }

    private def rerunCommandByPrefix(prefix: String): CommandAction =
    {
        history.get.reverse.filter(_.startsWith(prefix)) match
        {
            case Nil => 
                cmd.error("No command matches \"" + prefix + "\"")
                KeepGoing

            case line :: rest =>
                cmd.handleCommand(Some(line))
        }
    }
}

 

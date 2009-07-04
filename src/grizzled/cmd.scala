/*---------------------------------------------------------------------------*\
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
\*---------------------------------------------------------------------------*/

/**
 * Classes and objects to aid in the construction of line-oriented command
 * interpreters. This package is very similar, in concept, to the Python
 * <tt>cmd</tt> module (though its implementation differs quite a bit).
 */
package grizzled.cmd

import grizzled.readline.Readline.ReadlineType._
import grizzled.readline.Readline.ReadlineType
import grizzled.readline.{Readline, Completer, History}

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

    /**
     * The help for this command. The help string is written as is to the
     * screen. It is not wrapped, indented, or otherwise reformatted. It
     * may be a single string or a multiline string.
     */
    val help: String

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
     */
    def runCommand(command: String, unparsedArgs: String): Unit

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
    private[this] def allNames = name :: aliases
}

/**
 * Base class of any command interpreter.
 */
abstract class CommandInterpreter(val appName: String,
                                  readlineCandidates: List[ReadlineType])
{
    /**
     * For sorting names.
     */
    private lazy val NameSorter = (a: String, b: String) => a < b

    /**
     * The readline implementation being used.
     */
    val readline = findReadline(readlineCandidates)

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

        private val OutputWidth = 79

        private def helpHelp =
        {
            import scala.collection.mutable.ArrayBuffer

            // Help only.

            val commandNames = sortedCommandNames(false)

            println("Help is available for the following commands:")
            println("-" * OutputWidth)

            // Lay them out in columns. Simple-minded for now.
            val colSize = (0 /: commandNames.map(_.length)) (Math.max(_, _)) + 2
            val colsPerLine = OutputWidth / colSize
            for ((name, i) <- commandNames.zipWithIndex)
            {
                if ((i % colsPerLine) == 0)
                    print("\n")
                val padding = " " * (colSize - name.length)
                print(name + padding)
            }

            print("\n")
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

        def runCommand(command: String, unparsedArgs: String): Unit =
        {
            if (unparsedArgs == "")
                helpHelp
            else
                helpCommand(CmdUtil.tokenize(unparsedArgs))
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
        def process(line: String)
        {
            val (commandName, unparsedArgs) = splitCommandAndArgs(line)

            findCommand(commandName) match
            {
                case None  => 
                    println("*** Unknown command: " + commandName)
                case Some(handler) => 
                    if (handler.moreInputNeeded(line))
                        readAndProcess(line, secondaryPrompt)
                    else
                        handler.runCommand(commandName, unparsedArgs)
            }
        }

        def readAndProcess(prefix: String, prompt: String): Unit =
        {
            readline.readline(prompt) match
            {
                case None => 
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
     * Split a command from its argument list, returning the command as
     * one string and the remaining argument string as the other string.
     *
     * @param line  the input type
     *
     * @return A (<i>commandName</i>, <i>argumentString</i>) 2-tuple
     */
    private def splitCommandAndArgs(line: String): (String, String) =
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

    /**
     * Get a list of all command names, sorted.
     *
     * @param includeAliases whether or not to include aliases
     *
     * @return All command names, sorted.
     */
    private def sortedCommandNames(includeAliases: Boolean): List[String] =
    {
        val namesOnly = allHandlers.map(_.name)
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
    def tokenize(commandLine: String): List[String] =
        commandLine.trim.split("[ \t]+").toList
}

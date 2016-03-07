/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright © 2009-2016, Brian M. Clapper
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

   * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

   * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

   * Neither the names "clapper.org", "Grizzled Scala Library", nor the
    names of its contributors may be used to endorse or promote products
    derived from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  ---------------------------------------------------------------------------
 */

/** Classes and objects to aid in the construction of line-oriented command
  * interpreters. This package is very similar, in concept, to the Python
  * `cmd` module (though its implementation differs quite a bit).
  */
package grizzled.cmd

import grizzled.readline.Readline.ReadlineType._
import grizzled.readline.Readline.ReadlineType
import grizzled.readline.{Readline,
                          CompletionToken,
                          Completer,
                          ListCompleter,
                          LineToken,
                          Delim,
                          Cursor,
                          History}

import grizzled.string.Implicits.String._

import scala.collection.mutable.Stack
import scala.annotation.tailrec

/** Actions returned by handlers.
  */
sealed abstract class CommandAction
case object KeepGoing extends CommandAction
case object Stop extends CommandAction

/** Mapped commands.
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

/** Trait for an object (or class) that handles a single command. All logic
  * for a given command is embodied in a single object that mixes in this
  * trait.
  */
trait CommandHandler {

  /** The name of the command. This name, or any of the aliases (see below)
    * will cause the command to be invoked.
    */
  val CommandName: String

  /** Additional aliases for the command, if any.
    */
  val aliases: List[String] = Nil

  /** Whether or not the command should be put in the history.
    */
  val storeInHistory = true

  /** Whether or not the command is hidden. Hidden commands don't show up
    * in the help list or the history. Using the `HiddenCommandHandler`
    * trait saves a lot of work.
    */
  val hidden = false

  /** The help for this command. The help string is written as is to the
    * screen. It is not wrapped, indented, or otherwise reformatted. It
    * may be a single string or a multiline string.
    */
  val Help: String

  /** Compares a command name (that the user typed in, for instance) to
    * this command's name. The default implementation of this method simply
    * forces both names to lower case before comparing them. Overridden
    * definitions of this method can apply other matching criteria.
    *
    * @param candidate  the candidate name to be compared with this one
    *
    * @return `true` if they match, `false` if not
    */
  def matches(candidate: String): Boolean = {
    allNames.filter(_.toLowerCase == candidate.toLowerCase) match {
      case Nil => false
      case _   => true
    }
  }

  /** Compares a prefix string to this command name and its aliases, to
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
    *         completed by `prefix`, or `Nil`.
    */
  def commandNameCompletions(prefix: String): List[String] =
    allNames.filter(_.toLowerCase startsWith prefix.toLowerCase)

  /** This method is called after a line is read that matches this command,
    * to determine whether more lines need to be read to finish the command.
    * The default implementation returns `false`, meaning a single
    * input line suffices for the entire command. Implementing classes or
    * objects can override this method to ensure that the command has a
    * required terminating character (e.g., a ";"), doesn't end with a line
    * continuation character (e.g., "\"), or whatever the syntax requires.
    *
    * @param lineSoFar the line read so far
    */
  def moreInputNeeded(lineSoFar: String): Boolean = false

  /** Handle the command. The first white space-delimited token in the command
    * string is guaranteed to match the name of this command, by the rules of
    * the `matches()` method.
    *
    * @param command      the command that invoked this handler
    * @param unparsedArgs the remainder of the unparsed command line
    *
    * @return `KeepGoing` to tell the main loop to continue,
    *         or `Stop` to tell the main loop to be done.
    */
  def runCommand(command: String, unparsedArgs: String): CommandAction

  /** Perform completion on the command, returning the possible
    * completions. This method has the exact same interface and use as the
    * `complete()` method in `grizzled.readline.Completer`.
    * Please see that trait for full documentation.
    *
    * @param token       the token being completed
    * @param context     the token context (i.e., list of parsed tokens,
    *                    with cursor)
    * @param commandLine the current unparsed input line, which includes the
    *                    token
    */
  def complete(token: String,
               context: List[CompletionToken],
               commandLine: String): List[String] = Nil

  /** Convenience method to retrieve the combined list of names and aliases.
    */
  private[this] def allNames = CommandName :: aliases
}

/** Mixed in to indicate no completions are available.
  */
trait NoCompletionsHandler extends CommandHandler {
  override final def commandNameCompletions(prefix: String): List[String] =
    Nil
}

/** The handler trait for a hidden command. Hidden commands don't show up in
  * the help or the history.
  */
trait HiddenCommandHandler extends NoCompletionsHandler {
  val Help = "<hidden>"
  override val hidden = true
  override val storeInHistory = false
}

/** A block command is one that consists of multiple input lines between
  * some sort of start and end indicator. Mixing this trait into a command
  * handler changes the behavior of the command reader when it see the
  * command. Instead of calling `moreInputNeeded()` to determine when to
  * stop reading, the command reader just keeps reading until it sees a
  * line matching the `EndCommand` regular expression.
  */
trait BlockCommandHandler extends NoCompletionsHandler {
  val EndCommand: scala.util.matching.Regex

  override final def moreInputNeeded(lineSoFar: String): Boolean = false
}

/** Base class for command interpreters.
  *
  * `CommandInterpreter` is the base class of any command interpreter. This
  * class and the `CommandHandler` trait provide a simple framework for
  * writing line-oriented command-interpreters. This framework is
  * conceptually similar to the Python `cmd` module and its `Cmd` class,
  * though the implementation differs substantially in places.
  *
  * For reading input from the console, `CommandInterpreter` will use of
  * any of the readline libraries supported by the `grizzled.readline`
  * package. All of those libraries support a persistent command history,
  * and most support command completion and command editing.
  *
  * A command line consists of an initial command name, followed by a list
  * of arguments to that command. The `CommandInterpreter` class's
  * command reader automatically separates the command and the remaining
  * arguments, via the `splitCommandAndArgs()` method. Parsing the
  * arguments is left to the actual command implementation. The rules for how
  * the command is split from the remainder of the input line are outlined
  * in the documentation for the `splitCommandAndArgs()` method.
  *
  * @param appName             the application name, used by some readline
  *                            libraries for key-binding
  * @param readlineCandidates  list of readline libraries to try to load, in
  *                            order. The `ReadlineType` values are
  *                            defined by the `grizzled.readline`
  *                            package.
  * @param tokenDelimiters     delimiters to use when tokenizing a line for
  *                            tab-completion.
  */
abstract class CommandInterpreter(val appName: String,
                                  readlineCandidates: List[ReadlineType],
                                  val tokenDelimiters: String = """ \t""") {
  private val interpreter = this

  /** Assumed output width of the screen.
    */
  val OutputWidth = 79

  /** For sorting names.
    */
  private lazy val NameSorter = (a: String, b: String) => a < b

  /** Default list of readline libraries to try, in order.
    */
  val DefaultReadlineLibraryList = List(ReadlineType.EditLine,
                                        ReadlineType.GNUReadline,
                                        ReadlineType.GetLine,
                                        ReadlineType.JLine,
                                        ReadlineType.Simple)

  /** The readline implementation being used.
    */
  private val readlineLibs = readlineCandidates match {
    case Nil => DefaultReadlineLibraryList
    case _   => readlineCandidates
  }

  private def NoReadlineLibrary =
    throw new Exception("Can't find a readline library.")

  val readline = Readline.findReadline(readlineLibs, appName, false)
                         .getOrElse(NoReadlineLibrary)

  // Make sure readline library does its cleanup, no matter what happens.
  Runtime.getRuntime.addShutdownHook(
    new Thread {
      override def run = readline.cleanup
    }
  )

  readline.completer = CommandCompleter

  /** Get the history object being used to record command history.
    *
    * @return the `grizzled.readline.History` object
    */
  val history: History = readline.history

  /** The stack of readers. Usually, there will be only one element on this
    * stack: The readline implementation's readline() function. But callers
    * can push additional readers on the stack. Each element on the stack is
    * a function that takes a prompt to display and returns a line of input.
    */
  private val readerStack = new Stack[(String) => Option[String]]
  readerStack.push(readline.readline)

  /** Alternate constructor taking a single readline implementation. Fails
    * if that readline implementation cannot be found.
    *
    * @param appName   application name
    * @param readline  readline implementation
    */
  def this(appName: String, readline: ReadlineType) =
    this(appName, List(readline))

  /** Alternate constructor that tries all known readline implementations,
    * in this order:
    *
    * -  GNU Readline
    * -  Editline
    * -  Getline
    * -  JLine
    * -  Simple (pure Java)
    *
    * @param appName   application name
    */
  def this(appName: String) =
    this(appName, Nil)

  /** The primary prompt string.
    */
  def primaryPrompt = "? "

  /** The second prompt string, used when additional input is being
    * retrieved.
    */
  def secondaryPrompt = "> "

  /** `StartCommandIdentifier` is the list of characters that are
    * permitted as the first character of a white space-delimited,
    * multicharacter command name. All other characters are assumed to
    * introduce single-character commands. Subclasses may override this value
    * to permit additional, or different, starting characters for
    * multicharacter command names. See the `splitCommandAndArgs()`
    * method for more details.
    */
  def StartCommandIdentifier = "abcdefghijklmnopqrstuvwxyz" +
                               "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                               "0123456789."

  /** List of handlers. The subclass must define this value to contain a
    * list of its handlers. The `allHandlers` property will combine
    * this list with the help handler to get the list of all handlers.
    * If you define your own help handler, you'll have to override the
    * `helpHandler` property to return your help handler, instead of
    * the default one.
    */
  val handlers: List[CommandHandler]

  /** Get all handlers. By default, this property combines the
    * `handlers` value with the default help handler,
    * `HelpHandler`. If you define your own help handler, you'll
    * have to override the `helpHandler` property to return your
    * help handler, instead of the default one.
    */
  final def allHandlers: List[CommandHandler] = helpHandler :: handlers

  /** Get the help handler. Override this property if you want to supply
    * your own help handler.
    */
  def helpHandler = HelpHandler

  /** Push a reader on the reader stack. The reader on top of the stack
    * is used until it returns `None` (indicating EOF). Then, it is
    * removed from the stack, and the next reader is used. When the only
    * reader remaining on the stack returns `None`, the command
    * interpreter signals an EOF condition to the subclass (by calling
    * `handleEOF()`).
    *
    * The reader is a simple function that takes a prompt string (which
    * it can choose to ignore) and returns a line of input
    * (`Some(input)`) or `None` for EOF. The line of input,
    * if returned, should not have a trailing newline.
    *
    * @param reader  the reader function
    */
  def pushReader(reader: (String) => Option[String]) =
    readerStack.push(reader)

  /** Emit an error message in a consistent way. May be overridden by
    * subclasses. The default implementation prints errors in red.
    *
    * @param message the message to emit
    */
  def error(message: String) =
    println(Console.RED + "Error: " + message + Console.RESET)

  /** Emit a warning message in a consistent way. May be overridden by
    * subclasses. The default implementation prints the message with the
    * prefix "Warning: ".
    *
    * @param message the message to emit
    */
  def warning(message: String) =
    println(Console.YELLOW + "Warning: " + message + Console.RESET)

  /** Called just before the main loop (`mainLoop()`) begins its
    * command loop, this hook method can be used for initialization. The
    * default implementation does nothing.
    */
  def preLoop: Unit = return

  /** Called immediately after the main loop (`mainLoop()`) ends
    * its command loop, this hook method can be used for cleanup. The
    * default implementation does nothing.
    */
  def postLoop: Unit = return

  /** Called just before a command line is interpreted, this hook method can
    * edit the command.
    *
    * @param commandLine  the command line
    *
    * @return The possibly edited command, Some("") to signal an empty
    *         command, or None to signal EOF.
    */
  def preCommand(commandLine: String): Option[String] = Some(commandLine)

  /** Called after a command line is interpreted. The default implementation
    * simply returns `KeepGoing`.
    *
    * @param command      the command that invoked this handler
    * @param unparsedArgs the remainder of the unparsed command line
    *
    * @return `KeepGoing` to tell the main loop to continue,
    *         or `Stop` to tell the main loop to be done.
    */
  def postCommand(command: String, unparsedArgs: String): CommandAction =
    return KeepGoing

  /** Called when an empty command line is entered in response to the
    * prompt. The default version of this method simply returns
    * `KeepGoing`.
    *
    * @return `KeepGoing` to tell the main loop to continue,
    *         or `Stop` to tell the main loop to be done.
    */
  def handleEmptyCommand: CommandAction = return KeepGoing

  /** Called when an end-of-file condition is encountered while reading a
    * command (On Unix-like systems, with some readline libraries, this
    * happens when the user pressed Ctrl-D). prompt. The default version
    * of this method simply returns `Stop`, causing the command
    * loop to exit.
    *
    * @return `KeepGoing` to tell the main loop to continue,
    *         or `Stop` to tell the main loop to be done.
    */
  def handleEOF: CommandAction = return Stop

  /** Called when a command is entered that isn't recognized. The default
    * version of this method prints an error message and returns
    * `KeepGoing`.
    *
    * @param commandName  the command name
    * @param unparsedArgs the command arguments
    *
    * @return `KeepGoing` to tell the main loop to continue,
    *         or `Stop` to tell the main loop to be done.
    */
  def handleUnknownCommand(commandName: String,
                           unparsedArgs: String): CommandAction = {
    error("Unknown command: " + commandName)
    KeepGoing
  }

  /** Called when an exception occurs during the main loop. This method
    * can handle the exception however it wants; it must return either
    * `KeepGoing` or `Stop`. The default version
    * of this method dumps the exception stack trace and returns
    * `Stop`.
    *
    * @param e  the exception
    *
    * @return `KeepGoing` to tell the main loop to continue,
    *         or `Stop` to tell the main loop to be done.
    */
  def handleException(e: Exception): CommandAction = {
    error("Exception: " + e.getClass.toString)
    e.printStackTrace(System.out)
    Stop
  }

  /** Split a command from its argument list, returning the command as
    * one string and the remaining unparsed argument string as the other
    * string. The commmand name is parsed from the remaining arguments
    * using the following rules:
    *
    * - If the first non-white character if the input line is in the
    *   `StartCommandIdentifier` string, then the command is assumed to be
    *   a identifier that is separated from the arguments by white space.
    * - If the first character if the input line is not in the
    *   `StartCommandIdentifier` string, then the command is assumed to be
    *   a single-character command, with the arguments immediately
    *   following the single character.
    *
    * The `StartCommandIdentifier` string is an overridable field
    * defined by this class, consisting of the characters permitted to start
    * a multicharacter command. By default, it consists of alphanumerics.
    * Subclasses may override it to permit additional, or different, starting
    * characters for multicharacter commands.
    *
    * For example, using the default identifier characters, this function
    * will break the following commands into command + arguments as shown:
    *
    * Input: `foo bar baz`
    * Result: Command `foo`, argument string `"bar baz"`
    *
    * Input: `!bar`
    * Result: Command `!` argument string `"bar"`
    *
    * Input: `? one two`
    * Result: Command `?` argument string `"one two"`
    *
    * Subclasses may override this method to parse commands differently.
    *
    * @param line  the input type
    *
    * @return A (''commandName'', ''argumentString'') 2-tuple
    */
  def splitCommandAndArgs(line: String): (String, String) = {
    // Strip the command name.
    val lTrimmed = line.ltrim

    if (lTrimmed == "")
      ("", "")

    else if (StartCommandIdentifier.indexOf(lTrimmed(0)) == -1)
      // Single character command.
      (lTrimmed(0).toString, (lTrimmed drop 1).ltrim)

    else {
      // First token consists of all characters in StartCommandIdentifier.

      val isCommandChar = (c: Char) => StartCommandIdentifier contains c
      val command = lTrimmed.takeWhile(isCommandChar).mkString("")
      val args = lTrimmed.dropWhile(isCommandChar).mkString("")

      (command, args.ltrim)
    }
  }

  /** Handles a command line, just as if it had been typed directly at the
    * prompt.
    *
    * @param commandLine  the command line
    *
    * @return `KeepGoing` to tell the main loop to continue,
    *         or `Stop` to tell the main loop to be done.
    */
  final def handleCommand(commandLine: Option[String]): CommandAction = {
    try {
      val commandLine2 = commandLine.flatMap(preCommand _)
      mapCommandLine(commandLine2) match {
        case EOFCommand  =>
          handleEOF

        case EmptyCommand =>
          handleEmptyCommand

        case UnknownCommand(commandLine, commandName, args) =>
          history += commandLine
        handleUnknownCommand(commandName, args)

        case KnownCommand(handler, commandLine, commandName, args) =>
          if (handler.storeInHistory)
            history += commandLine
        val action = handler.runCommand(commandName, args)
        if (action != Stop)
          postCommand(commandName, args)
        action
      }
    }

    catch {
      case e: Exception =>
        handleException(e)
    }
  }

  /** Repeatedly issue a prompt, accept input, parse an initial prefix from
    * the received input, and dispatch to execution handlers.
    */
  final def mainLoop: Unit = {
    preLoop
    try {
      readAndProcessCommand
    }

    finally {
      postLoop
    }
  }

  /** Default handler for help messages. To redefine how help is generated,
    * create your own handler and redefine the `helpHandler`
    * property to return it.
    */
  private[cmd] object HelpHandler extends CommandHandler {
    val CommandName = "help"
    override val aliases = List("?", ".help")
    override val Help = """This message"""

    private def helpHelp = {
      import grizzled.collection.Implicits.GrizzledLinearSeq

      // Help only.

      val commandNames = sortedCommandNames(false)

      println("Help is available for the following commands:")
      println("-" * OutputWidth)

      print(commandNames.columnarize(OutputWidth))
    }

    private def helpCommand(names: List[String]) {
      for (name <- names) {
        findCommand(name) match {
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

    def runCommand(command: String, unparsedArgs: String): CommandAction = {
      if (unparsedArgs == "")
        helpHelp
      else
        helpCommand(CmdUtil.tokenize(unparsedArgs))

      KeepGoing
    }

    override def complete(token: String,
                          allTokens: List[CompletionToken],
                          line: String): List[String] = {
      // Get these now, not before-hand, since it's entirely possible
      // for the caller to install new command handlers on the fly.
      val commandNames = sortedCommandNames(false)
      val commandNameCompleter = new ListCompleter(commandNames)

      allTokens match {
        case Nil =>
          assert(false) // shouldn't happen
        Nil

        case LineToken(help) :: Cursor :: rest =>
          Nil

        case LineToken(help) :: Delim :: Cursor :: rest =>
          commandNameCompleter.complete(token, allTokens, line)

        case LineToken(help) :: Delim :: LineToken(cmd) ::
        Cursor :: rest =>
          commandNameCompleter.complete(token, allTokens, line)

        case _ =>
          Nil
      }
    }
  }

  /** Readline completion handler. Conditionally completes command names
    * or defers to command handlers for individual completion.
    */
  private object CommandCompleter extends Completer {
    import grizzled.readline.{Cursor, Delim, LineToken}

    override def tokenDelimiters = interpreter.tokenDelimiters

    def complete(token: String,
                 allTokens: List[CompletionToken],
                 line: String): List[String] = {
      def completeForCommand(commandName: String): List[String] = {
        findCommand(commandName) match {
          case None          => Nil
          case Some(handler) => handler.complete(token, allTokens,
                                                 line)
        }
      }

      val allNames = sortedCommandNames(true)
      allTokens match {
        case Nil =>
          allNames

        case Cursor :: Nil =>
          allNames

        case LineToken(partialCommand) :: Cursor :: Nil =>
          matchingCommandsFor(partialCommand)

        case LineToken(command) :: Delim :: rest =>
          completeForCommand(command)

        case _ =>
          Nil
      }
    }

    private def matchingCommandsFor(token: String): List[String] = {
      val completions = {
        for {handler <- allHandlers
             completions = handler.commandNameCompletions(token)
             if (completions != Nil)}
        yield completions
      }.toList

      completions.flatten[String]
    }
  }

  /** Read a command line
    *
    * @return `Some(line)` if a line was read, or `None` for
    *         EOF. Note that an empty string results in `Some("")`,
    *         not `None`.
    */
  private def readCommand: Option[String] = {
    @tailrec def doRead(lineSoFar: String,
                        usePrompt: String): Option[String] = {
      assert (readerStack.size > 0)
      val reader = readerStack.top
      val readlineResult = reader(usePrompt) match {
        case None =>
          None
        case Some(line) if (lineSoFar == "") =>
          Some((line, line))
        case Some(line) =>
          Some((line, (List(lineSoFar, line) mkString "\n")))
      }

      readlineResult match {
        case None =>
          if (readerStack.size == 1)
            // EOF on readline (primary reader)
            None
          else {
            readerStack.pop
            doRead("", primaryPrompt)
          }

        case Some((line, fullLine)) if (fullLine.trim == "") =>
          Some("")

        case Some((line, fullLine)) =>
          val (name, args) = splitCommandAndArgs(fullLine)

        findCommand(name) match {
          case None =>
            Some(fullLine)

          case Some(h: BlockCommandHandler) =>
            // Keep going until end of block.
            if (h.EndCommand.findFirstIn(line) != None)
              Some(fullLine)
            else
              doRead(fullLine, secondaryPrompt)

          case Some(h) =>
            if (! h.moreInputNeeded(fullLine))
              Some(fullLine)
            else
              doRead(fullLine + " ", secondaryPrompt)
        }
      }
    }

    doRead("", primaryPrompt)
  }

  /** Map a command string to a `Command` object.
    *
    * @param commandLine  the command line
    *
    * @return the `Command` object
    */
  private def mapCommandLine(commandLine: Option[String]): Command = {
    commandLine match {
      case None =>
        EOFCommand

      case Some(line) if ((line == null) || (line.trim == "")) =>
        EmptyCommand

      case Some(line) =>
        val (commandName, args) = splitCommandAndArgs(line)
      findCommand(commandName) match {
        case None =>
          UnknownCommand(line, commandName, args)
        case Some(handler) =>
          KnownCommand(handler, line, commandName, args)
      }
    }
  }

  /** Read and process the next command.
    */
  @tailrec private def readAndProcessCommand: Unit = {
    if (handleCommand(readCommand) == KeepGoing)
      readAndProcessCommand
  }

  /** Given a command line, find the command handler that matches,
    * checking the name against the command handler name and its aliases.
    *
    * @param name  the command name
    *
    * @return An option containing the command handler, or `None` for no match
    */
  private[this] def findCommand(name: String): Option[CommandHandler] = {
    val command = allHandlers filter (_ matches name)
    assert(command.length < 2)
    command match {
      case Nil       => None
      case List(cmd) => Some(cmd)
    }
  }

  /** Get a list of all command names, sorted.
    *
    * @param includeAliases whether or not to include aliases
    *
    * @return All command names, sorted.
    */
  private def sortedCommandNames(includeAliases: Boolean): List[String] = {
    val namesOnly = allHandlers.filter(! _.hidden).map(_.CommandName)
    val allNames =
      if (includeAliases)
        // Extract the aliases, producing a list of lists of strings.
        // Then, flatten that list of lists into a single list of
        // strings.
        namesOnly ::: allHandlers.map(_.aliases).flatten[String]
      else
        namesOnly

    allNames.sortWith(NameSorter)
  }
}

/** Miscellaneous useful utility functions.
  */
object CmdUtil {
  def tokenize(unparsedArgs: String): List[String] = {
    val trimmed = unparsedArgs.trim
    if (trimmed == "")
      Nil
    else
      trimmed.split("[ \t]+").toList
  }
}

/** Simple history command handler.
  *
  * A simple "history" (alias: "h") handler that displays the history to
  * standard output. This history handler supports the following usage:
  *
  * {{{
  * history [n]
  * }}}
  *
  * Where ''n'' is the number of (most recent) history entries to show.
  * If absent, ''n'' defaults to the size of the history buffer.
  *
  * This handler is ''not'' installed by default. It is provided as a
  * convenience, for command interpreters to use if desired.
  */
class HistoryHandler(val cmd: CommandInterpreter) extends CommandHandler {
  val CommandName = "history"
  override val aliases = List("h")

  val history = cmd.history
  val Help =
    """Show history.
      |
      |Usage: history [-n] [regex]
      |
      |Where n is the number of recent history entries to show. If a regular
      |expression is supplied, then only those history entries that match the
      |regular expression are shown."""
      .stripMargin

  private val TotalRegex = """^-([0-9]+)$""".r

  def runCommand(commandName: String, unparsedArgs: String): CommandAction =  {
    import scala.util.matching.Regex
    import java.util.regex.PatternSyntaxException

    val tokens = CmdUtil.tokenize(unparsedArgs)
    val historyCommands = history.get

    def filterByRegex(strings: List[(String, Int)],
                      pat: String): List[(String, Int)] = {
      try {
        val re = new Regex("(?i)" + pat) // case insensitive
        strings.filter {tup => re.findFirstIn(tup._1) != None}
      }
      catch {
        case e: PatternSyntaxException => {
          error("\"" + pat + "\" is a bad regular " +
                "expression: " + e.getMessage)
          Nil
        }
      }
    }

    val toShow =
      tokens match {
        case TotalRegex(sTotal) :: Nil => {
          val n = sTotal.toInt
          historyCommands.zipWithIndex
                         .reverse
                         .slice(0, n)
                         .reverse
        }

        case TotalRegex(sTotal) :: regex :: Nil => {
          val n = sTotal.toInt
          filterByRegex(historyCommands.zipWithIndex, regex)
            .reverse
            .slice(0, n)
            .reverse
        }

        case regex :: Nil =>
          filterByRegex(historyCommands.zipWithIndex, regex)

        case Nil =>
          historyCommands.zipWithIndex

        case _ => {
          error("Usage: history [-n] [regex]")
          Nil
        }
      }

    if (toShow.length > 0) {
      for ((line, i) <- toShow)
        printf("%3d: %s\n", i + 1, line)
    }

    KeepGoing
  }
}

/** Simple "redo command" handler.
  *
  * A simple "redo" command handler that supports re-issuing a numbered
  * command from the history or the last command with a given prefix. For
  * example, it supports the following syntaxes:
  *
  * Input: `!10`, `! 10`, or `r 10`
  * Meaning: Repeat the 10th command in the history.</td>
  *
  * Input: `!ec`, `! ec`, or `r ec`
  * Meaning: Repeat the most recent command whose name begins with "ec"
  *
  * Input: `!!`, `! !`, `r`
  * Meaning: Repeat the last command.
  *
  * This handler is ''not'' installed by default. It is provided as a
  * convenience, for command interpreters to use if desired.
  */
class RedoHandler(val cmd: CommandInterpreter) extends CommandHandler {
  val CommandName = "r"
  override val aliases = List("!", "/")
  override val storeInHistory = false

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

  private val CommandNumber = """^([0-9]+)$""".r

  def runCommand(commandName: String, unparsedArgs: String): CommandAction =  {
    val lTrimmedArgs = unparsedArgs.ltrim
    val historyBuf = history.get

    if (historyBuf.length == 0) {
      cmd.error("Empty command history.")
      KeepGoing
    }

    else {
      val commandList =
        if (lTrimmedArgs == "")
          List(commandName)
        else
          List(commandName, lTrimmedArgs)

      val command =
        commandList match {
          case "r" :: Nil =>
            Some(historyBuf(historyBuf.length - 1))

          case "!!" :: Nil =>
            Some(historyBuf(historyBuf.length - 1))

          case "!" :: CommandNumber(sNum) :: Nil =>
            getByNum(historyBuf, sNum.toInt)

          case "!" :: name :: Nil =>
            getByPrefix(historyBuf, name)

          case "r" :: CommandNumber(sNum) :: Nil =>
            getByNum(historyBuf, sNum.toInt)

          case "r" :: name :: Nil =>
            getByPrefix(historyBuf, name)

          case _ =>
            cmd.error("Error in command. Try: .help r")
            None
        }

      if (command == None)
        KeepGoing
      else {
        println(command)
        cmd.handleCommand(command)
      }
    }
  }

  private def getByNum(historyBuf: List[String], num: Int): Option[String] = {
    if ( (num < 1) || (num > historyBuf.length) ) {
      cmd.error("No command has number " + num)
      None
    }
    else {
      Some(historyBuf(num - 1))
    }
  }

  private def getByPrefix(historyBuf: List[String],
                          prefix: String): Option[String] = {
    historyBuf.reverse.filter(_.startsWith(prefix)) match {
      case Nil =>
        cmd.error("No command matches \"" + prefix + "\"")
        None

      case line :: rest =>
        Some(line)
    }
  }
}



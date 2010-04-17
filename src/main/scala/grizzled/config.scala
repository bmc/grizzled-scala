/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2009, Brian M. Clapper
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

/**
 * Classes and objects to aid in the parsing of INI-style configuration
 * files. This package is similar, in concept, to the Python
 * `ConfigParser` module (though its implementation and capabilities
 * differ quite a bit).
 */
package grizzled.config

import grizzled.file.Includer
import grizzled.file.filter.BackslashContinuedLineIterator
import grizzled.string.template.UnixShellStringTemplate
import grizzled.string.implicits._

import scala.annotation.tailrec
import scala.collection.mutable.{Map => MutableMap}
import scala.io.Source
import scala.util.matching.Regex

/**
 * Base class for all configuration exceptions.
 */
class ConfigException(val message: String) extends Exception(message)

/**
 * Thrown when a duplicate section is encountered.
 */
class DuplicateSectionException(sectionName: String)
extends ConfigException("Duplication section name: \"" + sectionName + "\"")

/**
 * Thrown when a duplicate option is encountered.
 */
class DuplicateOptionException(sectionName: String, optionName: String)
extends ConfigException("Duplicate option \"" + optionName + "\" in " +
                            "section \"" + sectionName + "\"")

/**
 * Thrown when an expected section is not present in the confiruation.
 */
class NoSuchSectionException(sectionName: String)
extends ConfigException("Section \"" + sectionName + "\" does not exist.")

/**
 * Thrown when an expected option is not present in the confiruation.
 */
class NoSuchOptionException(sectionName: String, optionName: String)
extends ConfigException("Section \"" + sectionName + "\" does not have " +
                        "an option named \"" + optionName + "\".")

class SubstitutionException(sectionName: String, message: String)
extends ConfigException("Section \"" + sectionName + "\" has a " +
                        "substitution error: " + message)

class ConversionException(sectionName: String,
                          optionName: String,
                          value: String,
                          message: String)
extends ConfigException("Section \"" + sectionName + "\", option \"" +
                        optionName + "\": Conversion error for value \"" +
                        value + "\": " + message)

/**
 * Used as a wrapper to pass a section to callbacks.
 */
class Section(val name: String, val options: Map[String, String])

/**
 * An INI-style configuration file parser.
 *
 * `Configuration` implements an in-memory store for a configuration file
 * whose syntax is reminiscent of classic Windows .INI files, though with
 * many extensions.
 *
 * '''Syntax'''
 *
 * A configuration file is broken into sections, and each section is
 * introduced by a section name in brackets. For example:
 *
 * {{{
 * [main]
 * installation.directory=/usr/local/foo
 * program.directory: /usr/local/foo/programs
 *
 * [search]
 * searchCommand: find /usr/local/foo -type f -name '*.class'
 *
 * [display]
 * searchFailedMessage=Search failed, sorry.
 * }}}
 *
 * Notes and caveats:
 *
 * At least one section is required.
 *
 * Sections may be empty.
 *
 * It is an error to have any variable definitions before the first
 * section header.

 * The section names "system" and "env" are reserved. They don't really
 * exist, but they're used during variable substitution (see below)
 * to substitute from <tt>System.properties</tt> and the environment,
 * respectively.
 *
 * '''Section Name Syntax'''
 *
 * There can be any amount of whitespace before and after the brackets
 * in a section name; the whitespace is ignored. Section names may consist
 * of alphanumeric characters and underscores. Anything else is not
 * permitted.
 *
 * '''Variable Syntax'''
 *
 * Each section contains zero or more variable settings. Similar to a Java
 * `Properties` file, the variables are specified as name/value pairs,
 * separated by an equal sign ("=") or a colon (":"). Variable names are
 * case-sensitive by default, though the case-sensitivity (and other
 * aspects of the variable name) may be changed by subclassing
 * `Configuration` and providing your own version of the
 * `transformOptionName()` method. Variable names may contain
 * alphanumerics, underscores, and hyphens (-). Variable values may contain
 * anything at all. The parser ignores whitespace on either side of the "="
 * or ":"; that is, leading whitespace in the value is skipped. The way to
 * include leading whitespace in a value is escape the whitespace
 * characters with backslashes. (See below).
 *
 * '''Continuation Lines'''
 *
 * Variable definitions may span multiple lines; each line to be
 * continued must end with a backslash ("\") character, which escapes the
 * meaning of the newline, causing it to be treated like a space character.
 * The following line is treated as a logical continuation of the first
 * line. Unlike Java properties files, however, leading whitespace is
 * <i>not</i> removed from continued lines.
 *
 * Only variable definition lines may be continued. Section header
 * lines, comment lines (see below) and include directives (see below)
 * cannot span multiple lines.
 *
 * '''Expansions of Variable Values'''
 *
 * The configuration parser preprocesses each variable's value, replacing
 * embedded metacharacter sequences and substituting variable references.
 * You can use backslashes to escape the special characters that the parser
 * uses to recognize metacharacter and variable sequences; you can also use
 * single quotes. See ''Suppressing Metacharacter Expansion and Variable
 * Substitution'', below, for more details.
 *
 * '''Metacharacters'''
 *
 * The parser recognizes Java-style ASCII escape sequences `\t`, `\n`,
 * `\r`, `\\`, `\&nbsp;` (a backslash and a space), and `\\u`''xxxx'' are
 * recognized and converted to single characters. Note that metacharacter
 * expansion is performed ''before'' variable substitution.
 *
 * '''Variable Substitution'''
 *
 * A variable value can interpolate the values of other variables, using
 * a variable substitution syntax. The general form of a variable reference
 * is `${sectionName.varName}`.
 *
 * `sectionName` is the name of the section containing the variable to
 * substitute; if omitted, it defaults to the current section. `varName` is
 * the name of the variable to substitute.
 *
 * If a variable reference specifies a section name, the referenced section
 * must precede the current section. It is not possible to substitute the value
 * of a variable in a section that occurs later in the file.
 *
 * The section names "system" and "env" are reserved for special
 * "pseudosections."
 *
 * The "system" pseudosection is used to interpolate values from
 * `System.properties` For instance, `${system.user.home}` substitutes the
 * value of the `user.home` system property (typically, the home directory
 * of the user running the program). Similarly, `${system.user.name}`
 * substitutes the user's name.
 *
 * The "env" pseudosection is used to interpolate values from the
 * environment. On UNIX systems, for instance, `${env.HOME}` substitutes
 * user's home directory (and is, therefore, a synonym for
 * `${system.user.home}`. On some versions of Windows, `${env.USERNAME}`
 * will substitute the name of the user running the program. Note: On UNIX
 * systems, environment variable names are typically case-sensitive; for
 * instance, `${env.USER}` and `${env.user}` refer to different environment
 * variables. On Windows systems, environment variable names are typically
 * case-insensitive; `${env.USERNAME}` and `${env.username}` are
 * equivalent.
 *
 * '''Notes and caveats:'''
 *
 * `Configuration` uses the
 * `grizzled.string.template.UnixShellVariableSubstituter`
 * class to do variable substitution, so it honors all the syntax conventions
 * supported by that class.
 *
 * Variable substitutions are only permitted within variable values. They are
 * ignored in variable names, section names, include directives and comments.
 *
 * Variable substitution is performed ''after'' metacharacter expansion (so
 * don't include metacharacter sequences in your variable names).
 *
 * To include a literal "$" character in a variable value, escape it with a
 * backslash, e.g., "`var=value with \$ dollar sign`"
 *
 * '''Suppressing Metacharacter Expansion and Variable Substitution'''
 *
 * To prevent the parser from interpreting metacharacter sequences,
 * variable substitutions and other special characters, use the "->"
 * assignment operator, instead of ":" or "=".
 *
 * For example, suppose you want to set variable "prompt" to the
 * literal value "Enter value. To specify a newline, use \n." The following
 * configuration file line will do the trick:
 *
 * {{{
 * prompt -> Enter value. To specify a newline, use \n
 * }}}
 *
 * Similarly, to set variable "abc" to the literal string "${foo}"
 * suppressing the parser's attempts to expand "${foo}" as a variable
 * reference, you could use:
 *
 * {{{
 * abc -> ${foo}
 * }}}
 *
 * Note: It's also possible, though hairy, to escape the special meaning
 * of special characters via the backslash character. For instance, you can
 * escape the variable substitution lead-in character, '$', with a
 * backslash. e.g., "\$". This technique is not recommended, however,
 * because you have to double-escape any backslash characters that you want
 * to be preserved literally. For instance, to get "\t", you must specify
 * "\\\\t". To get a literal backslash, specify "\\\\". (Yes, that's four
 * backslashes, just to get a single unescaped one.) This double-escaping
 * is a regrettable side effect of how the configuration file parses
 * variable values: It makes two separate passes over the value (one for
 * metacharacter expansion and another for variable expansion). Each of
 * those passes honors and processes backslash escapes. This problem would
 * go away if the configuration file parser parsed both metacharacter
 * sequences and variable substitutions itself, in one pass. It doesn't
 * currently do that, because it uses the separate
 * `grizzled.string.template.UnixShellStringTemplate` class
 * `grizzled.GrizzledString.translateMetachars()` method to do the
 * variable substitution and metacharacter translation. In general, you're
 * better off just sticking with the "->" assignment operator.
 *
 * '''Includes'''
 *
 * A special include directive permits inline inclusion of another
 * configuration file. The include directive takes two forms:
 *
 * {{{
 * %include "path"
 * %include "URL"
 * }}}
 *
 * For example:
 *
 * {{{
 * %include "/home/bmc/mytools/common.cfg"
 * %include "http://configs.example.com/mytools/common.cfg"
 * }}}
 *
 * If the include path is not a URL, and is not an absolute path, its
 * location is relative to the file that's trying to include it.
 * 
 * The included file may contain any content that is valid for this
 * parser. It may contain just variable definitions (i.e., the contents of
 * a section, without the section header), or it may contain a complete
 * configuration file, with individual sections. Since
 * `Configuration` recognizes a variable syntax that is
 * essentially identical to Java's properties file syntax, it's also legal
 * to include a properties file, provided it's included within a valid
 * section.
 *
 * Note: Attempting to include a file from itself, either directly or
 * indirectly, will cause the parser to throw an exception.
 *
 * '''Comments and Blank Lines'''
 *
 * A comment line is a one whose first non-whitespace character is a "#".
 * A blank line is a line containing no content, or one containing only
 * white space. Blank lines and comments are ignored.
 *
 * '''Caller-supplied Predefined Sections'''
 *
 * Calling applications may supply predefined sections and options, in
 * the form of a map. These sections may then be used by other sections,
 * via variable references. The predefined sections are defined in a map of
 * maps. The outer map is keyed by predefined section name. The inner maps
 * consist of options and their values. For instance, to read a
 * configuration file, giving it access to certain command line parameters,
 * you could do something like this:
 *
 * {{{
 * object Foo
 * {
 *     def main(args: Array[String]) =
 *     {
 *         // You'd obviously want to do some real argument checking here.
 *         val configFile = args(0)
 *         val name = args(1)
 *         val ipAddress = args(2)
 *         val sections = Map("args" -> Map("name" -> name,
 *                                          "ip" -> ipAddress))
 *         val config = Configuration(configFile, sections)
 *         ..
 *     }
 * }
 * }}}
 *
 * @param predefinedSections  the predefined sections. An empty map means
 *                            there are no predefined sections.
 */
class Configuration(predefinedSections: Map[String, Map[String, String]])
{
    private val SpecialSections  = Set("env", "system")
    private val SectionName      = """([a-zA-Z0-9_]+)""".r
    private val ValidSection     = ("""^\s*\[""" +
                                    SectionName.toString +
                                    """\]\s*$""").r
    private val BadSectionFormat = """^\s*(\[[^\]]*)$""".r
    private val BadSectionName   = """^\s*\[(.*)\]\s*$""".r
    private val CommentLine      = """^\s*(#.*)$""".r
    private val BlankLine        = """^(\s*)$""".r
    private val VariableName     = """([a-zA-Z0-9_.]+)""".r
    private val RawAssignment    = ("""^\s*""" +
                                    VariableName.toString +
                                    """\s*->\s*(.*)$""").r
    private val Assignment       = ("""^\s*""" +
                                    VariableName.toString +
                                    """\s*[:=]\s*(.*)$""").r
    private val FullVariableRef  = (SectionName.toString + """\.""" +
                                    VariableName.toString).r

    private val sections = MutableMap.empty[String, MutableMap[String, String]]

    addSections(predefinedSections)

    /**
     * Alternate constructor for use when there are no predefined sections.
     */
    def this() = this(Map.empty[String, Map[String, String]])

    /**
     * Get the list of section names.
     *
     * @return the section names, in a iterator
     */
    def sectionNames: Iterator[String] = sections.keysIterator

    /**
     * Add a section to the configuration.
     *
     * @param sectionName  the new section's name
     *
     * @throws DuplicateSectionException if the section already exists
     */
    def addSection(sectionName: String): Unit =
    {
        if (sections contains sectionName)
            throw new DuplicateSectionException(sectionName)

        if (SpecialSections contains sectionName)
            throw new DuplicateSectionException(sectionName)

        sections(sectionName) = MutableMap.empty[String, String]
    }

    /**
     * Add an option name and value to a section.
     *
     * @param sectionName  the section name
     * @param optionName   the option name, which will be transformed
     * @param value        the option's value
     *
     * @throws NoSuchSectionException   if the section doesn't exist
     * @throws DuplicateOptionException if the option already exists in the
     *                                  section
     */
    def addOption(sectionName: String,
                  optionName: String,
                  value: String): Unit =
    {
        def validate(canonicalOptionName: String,
                     optionMap: MutableMap[String, String]): Unit =
        {
            if (SpecialSections contains sectionName)
                throw new ConfigException("Can't add an option to read-only " +
                                          "section \"" + sectionName + "\"")

            if (optionMap.contains(canonicalOptionName))
                throw new DuplicateOptionException(sectionName, optionName)
        }

        putOption(sectionName, optionName, value, validate)
    }

    /**
     * Put an option name and value to a section, overwriting any existing
     * instance of that option. Unlike `addOption()`, this method will
     * not throw `DuplicateOptionException`.
     *
     * @param sectionName  the section name
     * @param optionName   the option name, which will be transformed
     * @param value        the option's value
     *
     * @throws NoSuchSectionException   if the section doesn't exist
     */
    def setOption(sectionName: String,
                  optionName: String,
                  value: String): Unit =
    {
        putOption(sectionName, optionName, value, ((s, m) => ()))
    }

    /**
     * Works like `Map.get()`, returning `Some(string)` if the value
     * is found, `None` if not. Does not throw exceptions.
     *
     * @param sectionName  the section name
     * @param optionName   the option name
     *
     * @return `Some(value)` if the section and option exist, `None` if
     *         either the section or option cannot be found.
     */
    def get(sectionName: String, optionName: String): Option[String] =
    {
        sectionName match
        {
            case "env" =>
                val value = System.getenv(optionName)
                if (value == null)
                    None
                else
                    Some(value)

            case "system" =>
                val value = System.getProperties.getProperty(optionName)
                if (value == null)
                    None
                else
                    Some(value)

            case _ =>
                if (! hasSection(sectionName))
                    None
                else
                {
                    val options = sections(sectionName)
                    val canonicalOptionName = transformOptionName(optionName)
                    options.get(canonicalOptionName)
                }
        }
    }

    /**
     * Works like `Map.getOrElse()`, returning an option value or a
     * default, if the option has no value. Does not throw exceptions.
     *
     * @param sectionName  the section name
     * @param optionName   the option name
     * @param default      the default value
     *
     * @return The option's value if the section and option exist, the
     *         default if either the section or option cannot be found.
     */
    def getOrElse(sectionName: String,
                  optionName: String,
                  default: String): String =
    {
        get(sectionName, optionName) match
        {
            case Some(value) => value
            case None        => default
        }
    }

    /**
     * Get an optional integer option.
     *
     * @param sectionName  the section name
     * @param optionName   the option name
     *
     * @return `Some(integer)` or None.
     *
     * @throws ConversionException    if the option has a non-integer value
     */
    def getInt(sectionName: String, optionName: String): Option[Int] =
    {
        get(sectionName, optionName) match
        {
            case Some(value) =>
                try
                {
                    Some(value.toInt)
                }

                catch
                {
                    case _: NumberFormatException =>
                        throw new ConversionException(sectionName,
                                                      optionName,
                                                      value,
                                                      "not an integer.")
                }

            case None =>
                None
        }
    }

    /**
     * Get an integer option, applying a default if not found.
     *
     * @param sectionName  the section name
     * @param optionName   the option name
     * @param default      the default value
     *
     * @return the integer result
     *
     * @throws ConversionException    if the option cannot be converted
     */
    def getIntOrElse(sectionName: String,
                     optionName: String,
                     default: Int): Int =
    {
        getInt(sectionName, optionName) match
        {
            case Some(i) => i
            case None    => default
        }
    }

    /**
     * Get an optional boolean option.
     *
     * @param sectionName  the section name
     * @param optionName   the option name
     *
     * @return `Some(boolean)` or None.
     *
     * @throws ConversionException if the option has a non-boolean value
     */
    def getBoolean(sectionName: String, optionName: String): Option[Boolean] =
    {
        import grizzled.string.implicits._

        get(sectionName, optionName) match
        {
            case Some(value) =>
                try
                {
                    val b: Boolean = value
                    Some(b)
                }

                catch
                {
                    case _: IllegalArgumentException =>
                        throw new ConversionException(sectionName,
                                                      optionName,
                                                      value,
                                                      "not a boolean.")
                }

            case None =>
                None
        }
    }

    /**
     * Get a boolean option, applying a default if not found.
     *
     * @param sectionName  the section name
     * @param optionName   the option name
     * @param default      the default value
     *
     * @return the integer result
     *
     * @throws ConversionException    if the option cannot be converted
     */
    def getBooleanOrElse(sectionName: String,
                         optionName: String,
                         default: Boolean): Boolean =
    {
        getBoolean(sectionName, optionName) match
        {
            case Some(b) => b
            case None    => default
        }
    }

    /**
     * Determine whether the configuration contains a named section.
     *
     * @param sectionName  the new section's name
     *
     * @return `true` if the configuration has a section with that name,
     *         `false` otherwise
     */
    def hasSection(sectionName: String): Boolean =
        sections contains sectionName

    /**
     * Get the value for an option in a section.
     *
     * @param sectionName  the section name
     * @param optionName   the option name
     *
     * @return the option's value
     *
     * @throws NoSuchSectionException if the section doesn't exist
     * @throws NoSuchOptionException  if the option doesn't exist
     *
     * @deprecated  Use get() or getOrElse(), instead
     */
    def option(sectionName: String, optionName: String): String =
    {
        if ((! SpecialSections.contains(sectionName)) &&
            (! hasSection(sectionName)))
            throw new NoSuchSectionException(sectionName)

        get(sectionName, optionName) match
        {
            case None =>
                throw new NoSuchOptionException(sectionName, optionName)
            case Some(value) =>
                value
        }
    }

    /**
     * Get the value for an option in a section, supplying a default if the
     * option or the section doesn't exist. Exceptions for variable
     * substitution errors are still thrown.
     *
     * @param sectionName  the section name
     * @param optionName   the option name
     * @param default      default value
     *
     * @return the option's value (which may be the default)
     *
     * @deprecated  Use get() or getOrElse(), instead
     */
    def option(sectionName: String,
               optionName: String,
               default: String): String =
    {
        try
        {
            option(sectionName, optionName)
        }

        catch
        {
            case _: NoSuchOptionException => default
            case _: NoSuchSectionException => default
        }
    }

    /**
     * Get all options in a section.
     *
     * @param sectionName  the section name
     *
     * @return a map of all options and their values for the section. If
     *         the section doesn't exist, an empty map is returned.
     *
     * @throws NoSuchSectionException if the section doesn't exist
     */
    def options(sectionName: String): Map[String, String] =
        if (hasSection(sectionName))
            Map.empty[String, String] ++ sections(sectionName)
        else
            Map.empty[String, String]

    /**
     * Get the list of option names.
     *
     * @param sectionName the section's name
     *
     * @return a list of option names in that section
     *
     * @throws NoSuchSectionException if the section doesn't exist
     */
    def optionNames(sectionName: String): Iterator[String] =
    {
        if (! hasSection(sectionName))
            throw new NoSuchSectionException(sectionName)

        sections(sectionName).keysIterator
    }

    /**
     * Transform an option (key) to a consistent form. The default
     * version of this method forces the option name to lower case.
     *
     * @param option  the option name
     *
     * @return the transformed option name
     */
    def transformOptionName(option: String) = option.toLowerCase

    /**
     * Invoke a code block on each section whose name matches a regular
     * expression.
     *
     * @param regex  the regular expression to match
     * @param code   the block of code to invoke with each section
     */
    def forMatchingSections(regex: Regex)(code: Section => Unit) =
    {
        for (name <- sectionNames; if (regex.findFirstIn(name) != None))
            code(new Section(name, options(name)))
    }

    /**
     * Return a sequence of sections whose name match matches a regular
     * expression.
     *
     * @param regex  the regular expression to match
     */
    def matchingSections(regex: Regex): Seq[Section] =
    {
        for (name <- sectionNames; if (regex.findFirstIn(name) != None))
            yield new Section(name, options(name))
    }.toSeq

    /**
     * Load configuration data from the specified source into this object.
     * Clears the configuration first.
     *
     * @param source  `scala.io.Source` object to read
     * @param safe    Whether an exception should be thrown for bad variable
     *                substitutions (`false`) or not (`true`).
     *
     * @return this object, for convenience
     */
    def load(source: Source, safe: Boolean = false): Configuration =
    {
        def processLine(line: String,
                        curSection: Option[String]): Option[String] =
        {
            def unsafeResolveVariable(name: String): Option[String] =
                getVar(curSection.get, name)

            def safeResolveVariable(name: String): Option[String] =
                safeGetVar(curSection.get, name)

            val resolve = if (safe) safeResolveVariable _
                          else unsafeResolveVariable _

            line match
            {
                case CommentLine(_) =>
                    curSection

                case BlankLine(_) =>
                    curSection

                case ValidSection(name) =>
                    addSection(name)
                    Some(name)

                case BadSectionFormat(section) =>
                    throw new ConfigException("Badly formatted section: \"" +
                                              section + "\"")

                case BadSectionName(name) =>
                    throw new ConfigException("Bad section name: \"%s\"" + name)

                case Assignment(optionName, value) =>
                    val template = new UnixShellStringTemplate(resolve,
                                                               "[a-zA-Z0-9_.]+",
                                                               safe)
                    if (curSection == None)
                        throw new ConfigException("Assignment \"" +
                                                  optionName + "=" + value +
                                                  "\" occurs before the " +
                                                  "first section.")
                    val newValue = template.substitute(value.translateMetachars)
                    addOption(curSection.get, optionName, newValue)
                    curSection

                case RawAssignment(optionName, value) =>
                    if (curSection == None)
                        throw new ConfigException("Assignment \"" +
                                                  optionName + "=" + value +
                                                  "\" occurs before the " +
                                                  "first section.")
                    addOption(curSection.get, optionName, value)
                    curSection

                case _ =>
                    throw new ConfigException("Unknown configuration line: \"" +
                                              line + "\"")
            }
        }

        @tailrec def processLines(lines: Iterator[String],
                                  curSection: Option[String]): Unit =
        {
            if (lines.hasNext)
            {
                val nextSection = processLine(lines.next, curSection)
                processLines(lines, nextSection)
            }
        }

        processLines(new BackslashContinuedLineIterator(Includer(source)), None)
        this
    }

    /**
     * Puts an option in the configuration, running the specified pre-check
     * logic first.
     */
    protected def putOption
        (sectionName: String,
         optionName: String,
         value: String,
         preCheck: (String, MutableMap[String, String]) => Unit): Unit =
    {
        if (! hasSection(sectionName))
            throw new NoSuchSectionException(sectionName)

        val optionMap = sections(sectionName)
        val canonicalOptionName = transformOptionName(optionName)

        preCheck(canonicalOptionName, optionMap)

        optionMap += (canonicalOptionName -> value)
    }

    private def safeGetVar(curSection: String, 
                           varName: String): Option[String] =
    {
        try
        {
            getVar(curSection, varName)
        }

        catch
        {
            case _: NoSuchOptionException => None
            case _: NoSuchSectionException => None
        }
    }

    private def getVar(curSection: String, varName: String): Option[String] =
    {
        try
        {
            varName match
            {
                case FullVariableRef(section, option) =>
                    Some(this.option(section, option))
                case VariableName(option) =>
                    Some(this.option(curSection, option))
                case _ =>
                    throw new SubstitutionException(curSection,
                                                    "Reference to" +
                                                    "nonexistent section or " +
                                                    "option: ${ " +
                                                    varName + "}")
            }
        }

        catch
        {
            case _: NoSuchOptionException =>
                throw new SubstitutionException(curSection,
                                                "Reference to nonexistent " +
                                                "option in ${" + varName + "}")

            case _: NoSuchSectionException =>
                throw new SubstitutionException(curSection,
                                                "Reference to nonexistent " +
                                                "section in ${" + varName + "}")
        }
    }

    private def addSections(newSections: Map[String,Map[String,String]]): Unit =
    {
        for ((sectionName, optionMap) <- predefinedSections)
        {
            addSection(sectionName)
            for ((optionName, optionValue) <- optionMap)
                addOption(sectionName, optionName, optionValue)
        }
    }
}

/**
 * A configuration reader: Reads a source and produces a parsed
 * configuration.
 */
object ConfigurationReader
{
    /**
     * Read a configuration.
     *
     * @param source `scala.io.Source` object to read
     *
     * @return the `Configuration` object.
     *
     * @deprecated Use the Configuration object
     */
    def read(source: Source): Configuration =
        read(source, Map.empty[String, Map[String, String]])

    /**
     * Read a configuration file, permitting some predefined sections to be
     * added to the configuration before it is read. The predefined sections
     * are defined in a map of maps. The outer map is keyed by predefined
     * section name. The inner maps consist of options and their values.
     * For instance, to read a configuration file, giving it access to
     * certain command line parameters, you could do something like this:
     *
     * {{{
     * object Foo
     * {
     *     def main(args: Array[String]) =
     *     {
     *         // You'd obviously want to do some real argument checking here.
     *         val configFile = args(0)
     *         val name = args(1)
     *         val ipAddress = args(2)
     *         val sections = Map("args" -> Map("name" -> name,
     *                                          "ip" -> ipAddress))
     *         val config = Configuration(configFile, sections)
     *         ..
     *     }
     * }
     * }}}
     *
     * @param source    `scala.io.Source` object to read
     * @param sections  the predefined sections. An empty map means there are
     *                  no predefined sections.
     *
     * @return the `Configuration` object.
     *
     * @deprecated Use the Configuration object
     */
    def read(source: Source,
             sections: Map[String, Map[String, String]]): Configuration =
    {
        new Configuration(sections).load(source)
    }
}

/**
 * Companion object for the `Configuration` class
 */
object Configuration
{
    import java.io.File

    /**
     * Read a configuration file.
     *
     * @param source `scala.io.Source` object to read
     * @param safe    Whether an exception should be thrown for bad variable
     *                substitutions (`false`) or not (`true`).
     *
     * @return the `Configuration` object.
     */
    def apply(source: Source, safe: Boolean = false): Configuration =
        new Configuration().load(source, safe)

    /**
     * Read a configuration file, permitting some predefined sections to be
     * added to the configuration before it is read. The predefined sections
     * are defined in a map of maps. The outer map is keyed by predefined
     * section name. The inner maps consist of options and their values.
     * For instance, to read a configuration file, giving it access to
     * certain command line parameters, you could do something like this:
     *
     * {{{
     * object Foo
     * {
     *     def main(args: Array[String]) =
     *     {
     *         // You'd obviously want to do some real argument checking here.
     *         val configFile = args(0)
     *         val name = args(1)
     *         val ipAddress = args(2)
     *         val sections = Map("args" -> Map("name" -> name,
     *                                          "ip" -> ipAddress))
     *         val config = Configuration(Source.fromFile(new File(configFile)), sections)
     *         ..
     *     }
     * }
     * }}}
     *
     * @param source    `scala.io.Source` object to read
     * @param sections  the predefined sections. An empty map means there are
     *                  no predefined sections.
     *
     * @return the `Configuration` object.
     */
    def apply(source: Source,
              sections: Map[String, Map[String, String]]): Configuration =
        new Configuration(sections).load(source)

    /**
     * Read a configuration file, permitting some predefined sections to be
     * added to the configuration before it is read. The predefined sections
     * are defined in a map of maps. The outer map is keyed by predefined
     * section name. The inner maps consist of options and their values.
     * For instance, to read a configuration file, giving it access to
     * certain command line parameters, you could do something like this:
     *
     * {{{
     * object Foo
     * {
     *     def main(args: Array[String]) =
     *     {
     *         // You'd obviously want to do some real argument checking here.
     *         val configFile = args(0)
     *         val name = args(1)
     *         val ipAddress = args(2)
     *         val sections = Map("args" -> Map("name" -> name,
     *                                          "ip" -> ipAddress))
     *         val config = Configuration(Source.fromFile(new File(configFile)), sections)
     *         ..
     *     }
     * }
     * }}}
     *
     * @param source    `scala.io.Source` object to read
     * @param sections  the predefined sections. An empty map means there are
     *                  no predefined sections.
     * @param safe      Whether an exception should be thrown for bad variable
     *                  substitutions (`false`) or not (`true`).
     *
     * @return the `Configuration` object.
     */
    def apply(source: Source,
              sections: Map[String, Map[String, String]],
              safe: Boolean): Configuration =
        new Configuration(sections).load(source, safe)
}

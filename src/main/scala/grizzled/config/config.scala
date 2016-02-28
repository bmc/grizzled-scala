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
import grizzled.string.GrizzledString._
import grizzled.either.Implicits._

import scala.annotation.tailrec
import scala.io.Source
import scala.util.matching.Regex
import scala.util.{Try, Success, Failure}

/** Some commonly used type aliases
  */
package object Types {
  type NotFoundFunction = (String, String) => Either[String, Option[String]]
}

/**
  * Used as a wrapper to pass a section to callbacks.
  */
class Section(val name: String, val options: Map[String, String]) {
  override def toString = "[" + name + "]"
}

/** To create your own value converter, implement this trait.
  *
  * @tparam T the type the converter returns
  */
trait ValueConverter[T] {
  /** Convert an option value to the appropriate type.
    *
    * @param sectionName the name of the section, for error messages
    * @param optionName  the name of the option, for error messages
    * @param value       the option's value, as a string
    *
    * @return `Right(value)` on success; `Left(error)` on error
    */
  def convert(sectionName: String,
              optionName:  String,
              value:       String): Either[String, T]
}

/** An INI-style configuration file parser.
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
  * to substitute from `System.properties` and the environment,
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
  * ''not'' removed from continued lines.
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
  * `\r`, `\\`, `\ ` (a backslash and a space), and `\``u`''xxxx'' are
  * recognized and converted to single characters. Note that metacharacter
  * expansion is performed ''before'' variable substitution.
  *
  * '''Variable Substitution'''
  *
  * A variable value can interpolate the values of other variables, using
  * a variable substitution syntax. The general form of a variable reference
  * is `\${sectionName.varName}`.
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
  * `System.properties` For instance, `\${system.user.home}` substitutes the
  * value of the `user.home` system property (typically, the home directory
  * of the user running the program). Similarly, `\${system.user.name}`
  * substitutes the user's name.
  *
  * The "env" pseudosection is used to interpolate values from the
  * environment. On UNIX systems, for instance, `\${env.HOME}` substitutes
  * user's home directory (and is, therefore, a synonym for
  * `\${system.user.home}`. On some versions of Windows, `\${env.USERNAME}`
  * will substitute the name of the user running the program. Note: On UNIX
  * systems, environment variable names are typically case-sensitive; for
  * instance, `\${env.USER}` and `\${env.user}` refer to different environment
  * variables. On Windows systems, environment variable names are typically
  * case-insensitive; `\${env.USERNAME}` and `\${env.username}` are
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
  * To include a literal "\$" character in a variable value, escape it with a
  * backslash, e.g., "`var=value with \\$ dollar sign`"
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
  * Similarly, to set variable "abc" to the literal string "\${foo}"
  * suppressing the parser's attempts to expand "\${foo}" as a variable
  * reference, you could use:
  *
  * {{{
  * abc -> \${foo}
  * }}}
  *
  * Note: It's also possible, though hairy, to escape the special meaning
  * of special characters via the backslash character. For instance, you can
  * escape the variable substitution lead-in character, '\$', with a
  * backslash. e.g., "\\$". This technique is not recommended, however,
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
  * <pre>
  * def main(args: Array[String]): Unit = {
  *   val configFile = args(0)
  *   val name = args(1)
  *   val ipAddress = args(2)
  *   val sections = Map("args" -> Map("name" -> name, "ip" -> ipAddress))
  *   val config = Configuration(configFile, sections)
  *   ...
  * }
  * </pre>
  *
  * Note that contents of the configuration file can override the predefined
  * sections.
  *
  * Applications may also provide a "not found" function that is called to
  * resolveOpt options that are not found in the table. Such a function can be
  * used to supply on-demand sections and values. For example, suppose you
  * want to do something crazy, such as look up any not-found values in a
  * database. (This is probably a very bad idea, but it makes a good example.)
  * You might do something like this:
  * {{{
  * def findInDatabase(sectionName: String, optionName: String):
  *   Either[String, Option[String]] = {
  *
  *   val select = "SELECT value FROM config WHERE section = ? and option = ?"
  *   ...
  * }
  *
  * val config = Configuration(configFile, notFoundFunction = findInDatabase)
  * }}}
  *
  * @param contents            the predefined sections. An empty map means
  *                            there are no predefined sections.
  * @param sectionNamePattern  Regular expression that matches legal section
  *                            names. The section name portion must be in
  *                            a group. Default: ([a-zA-Z0-9_]+)
  * @param commentPattern      Regular expression that matches comment lines.
  *                            Default: ^\s*(#.*)$
  * @param normalizeOptionName function to call to convert an option name to
  *                            a key
  * @param notFoundFunction    function to call if an option is not found,
  *                            or None
  * @param safe                `true` does "safe" substitutions, with
  *                            substitutions of nonexistent values replaced by
  *                            empty strings. `false` ensures that bad
  *                            substitutions result in errors (or `None` in
  *                            functions, like `get()`, that return `Option`
  *                            values).
  */
final class Configuration private[config](
  private val contents:             Map[String, Map[String, Value]],
  private val sectionNamePattern:   Regex,
  private val commentPattern:       Regex,
  private val normalizeOptionName:  (String => String),
  private val notFoundFunction:     Option[Types.NotFoundFunction] = None,
  private val safe:                 Boolean = true) {

  private val SectionName      = sectionNamePattern
  private val VariableName     = """([a-zA-Z0-9_.]+)""".r
  private val FullVariableRef  = (SectionName.toString + """\.""" +
                                  VariableName.toString).r

  /** Key used for an option, allowing storage of original option string and
    * transformed value.
    */
  private case class OptionKey(originalKey: String) {
    val transformedKey = normalizeOptionName(originalKey)

    override def equals(other: Any) = {
      other match {
        case k: OptionKey => k.transformedKey == transformedKey
        case _            => false
      }
    }

    override def hashCode = transformedKey.hashCode

    override lazy val toString = s"OptionKey<$originalKey, $transformedKey>"
  }

  private val sections = contents.map { case (key, options) =>
    key -> options.map { case (name, value) => OptionKey(name) -> value }
  }

  /** Get the list of section names.
    *
    * @return the section names, in a iterator
    */
  def sectionNames: Iterator[String] = sections.keysIterator

  /** Get a section. Similar to `Map.get`, this method returns `Some(Section)`
    * if the section exists, and `None` if it does not.
    *
    * @param name  the section to get
    *
    * @return `Some(Section)` or `None`
    */
  def getSection(name: String): Option[Section] = {
    sections.get(name).map { m =>
      val sectionMap = m.map {
        case (option, value) =>
          option -> resolveOpt(name, value.value)
      }.
      filter { case (key, value) =>
        value.isDefined
      }.
      map { case (key, optValue) =>
        // At this point, courtesy of the filter(), we know the option is
        // not None.
        key.originalKey -> optValue.get
      }

      new Section(name, sectionMap)
    }
  }

  /** Works like `Map.get()`, returning `Some(string)` if the value
    * is found, `None` if not. Does not throw exceptions.
    *
    * @param sectionName  the section name
    * @param optionName   the option name
    *
    * @return `Some(value)` if the section and option exist, `None` if
    *         either the section or option cannot be found.
    */
  def get(sectionName: String, optionName: String): Option[String] = {

    def handleNotFound(): Option[String] = {
      notFoundFunction.flatMap { f =>
        f(sectionName, optionName) match {
          case Left(error) => None
          case Right(None) => None
          case Right(s)    => s
        }
      }
    }

    sectionName match {
      case "env" =>
        Option(System.getenv(optionName))

      case "system" =>
        Option(System.getProperties.getProperty(optionName))

      case _ if ! hasSection(sectionName) =>
        handleNotFound()

      case _ => {
        val key = OptionKey(optionName)
        val res: Option[String] = sections(sectionName).get(key) match {
          case Some(v) => Some(v.value)
          case None    => handleNotFound()
        }

        res.flatMap { v => resolveOpt(sectionName, v) }
      }
    }
  }

  /** Like `get()`, except that this method returns an `Either`, allowing
    * errors to be captured and processed.
    *
    * @param sectionName  the section name
    * @param optionName   the option name
    *
    * @return `Left(error)` on error. `Right(None)` if not found.
    *         `Right(Some(value))` if found and processed.
    */
  def getEither(sectionName: String, optionName: String):
    Either[String, Option[String]] = {

    sectionName match {
      case "env" =>
        Right(Option(System.getenv(optionName)))

      case "system" =>
        Right(Option(System.getProperties.getProperty(optionName)))

      case _ if ! hasSection(sectionName) =>
        Right(None)

      case _ => {
        val key = OptionKey(optionName)
        val res = sections(sectionName).get(key).map (_.value)

        res.map { resolveEither(sectionName, _) }.getOrElse(Right(None))
      }
    }
  }

  /** Get a value as an instance of specified type. This method retrieves the
    * value of an option from a section and, using the specified (or implicit)
    * converter, attempts to convert the option's to the specified type. If you
    * import `grizzled.config.Configuration.Implicits._`, you'll bring implicit
    * converters for various common types into scope.
    *
    * @param sectionName  the section from which to retrieve the value
    * @param optionName   the name of the option whose value is to be returned
    * @tparam T           the desired type of the result
    * @param converter    a `ValueConverter` object that will handle the
    *                     actual conversion.
    *
    * @return `None` if not found or not convertible, `Some(value)` if found
    *         and converted. If you want to distinguish between "not found" and
    *         "cannot convert", use `asEither()`.
    */
  def asOpt[T](sectionName: String, optionName: String)
              (implicit converter: ValueConverter[T]): Option[T] = {
    asEither(sectionName, optionName)(converter) match {
      case Left(error) => None
      case Right(opt)  => opt
    }
  }

  /** Get a value as an instance of specified type. This method retrieves the
    * value of an option from a section and, using the specified (or implicit)
    * converter, attempts to convert the option's to the specified type. If you
    * import `grizzled.config.Configuration.Implicits._`, you'll bring implicit
    * converters for various common types into scope.
    *
    * If `safe` is `true` (as defined when the `Configuration` object is built),
    * substitutions of nonexistent variables will result in empty strings for
    * where the substitutions were specified (e.g., `val\${section1.notValid}`
    * will result in the string "val"). If `safe` is `false`, substitutions
    * of nonexistent values will result in an error (i.e., a `Left` result).
    *
    *@param sectionName  the section from which to retrieve the value
    * @param optionName   the name of the option whose value is to be returned
    * @tparam T           the desired type of the result
    * @param converter    a `ValueConverter` object that will handle the
    *                     actual conversion.
    *
    * @return `Left(error)` on conversion error. `Right(None)` if not found.
    *         `Right(Some(value))` if found and converted.
    */
  def asEither[T](sectionName: String, optionName: String)
                 (implicit converter: ValueConverter[T]):
    Either[String, Option[T]] = {

    getEither(sectionName, optionName).flatMap { valueOpt: Option[String] =>
      valueOpt.map { value =>
        // Converter returns an Either[String, T]. We need to map it to an
        // Either[String, Option[T]]
        converter.convert(sectionName, optionName, value).map {Some(_)}
      }.
      getOrElse(Right(None))
    }
  }

  /** Works like `Map.getOrElse()`, returning an option value or a
    * default, if the option has no value. Does not throw exceptions.
    * Calling this function is the same as:
    * {{{
    * get(sectionName, optionName).getOrElse(default)
    * }}}
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
                default: String): String = {
    get(sectionName, optionName).getOrElse(default)
  }

  /** Retrieve a value, splitting it into a list of strings.
    * Returns `Some(list)` if the key is found, and `None` otherwise.
    *
    * @param sectionName   the section name
    * @param optionName    the option name
    * @param separators    separator regex to use. Default: [\s,]
    */
  def getAsList(sectionName: String,
                optionName: String,
                separators: Regex = """[\s,]""".r): Option[List[String]] = {

    get(sectionName, optionName).map(s =>
      separators.split(s).filter(_.length > 0)
    ).map(_.toList)
  }

  /** Add a value to the configuration, returning a new object. If the
    * option already exists in the specified section, it is replaced in
    * the new configuration. Otherwise, it's added. If the section doesn't
    * exist, it's created and the option is added.
    *
    * Example:
    * {{{
    *   val cfg = Configuration(...)
    *   val newCfg = cfg + ("myNewSection", "optionName", "value")
    * }}}
    *
    * @param section  the section name
    * @param option   the option name
    * @param value    the value
    *
    * @return a new `Configuration` object with the change applied.
    */
  def +(section: String, option: String, value: String): Configuration = {
    val existing = contents.get(section)
    val newSection = existing.map { sectionMap =>
      sectionMap + (option -> Value(value))
    }.
    getOrElse(Map(option -> Value(value)))

    val newContents = contents + (section -> newSection)
    new Configuration(contents            = newContents,
                      sectionNamePattern  = this.sectionNamePattern,
                      commentPattern      = this.commentPattern,
                      normalizeOptionName = this.normalizeOptionName,
                      notFoundFunction    = this.notFoundFunction,
                      safe                = this.safe)
  }

  /** Add multiple (section -> (option -> value)) triplets to the configuration,
    * returning the new configuration. Example use:
    *
    * {{{
    *   val cfg = Configuration(...)
    *   val newCfg = cfg ++ (("newSection1" -> ("option1" -> "value1")),
    *                        ("newSection2" -> ("option1" -> "value1")),
    *                        ("newSection1" -> ("option3" -> "value3")))
    * }}}
    *
    * @param values one or more (section -> (option -> value)) triplets
    *
    * @return new configuration
    */
  def ++(values: (String, (String, String))*): Configuration = {
    // Broken into pieces for easier reading. Types added for the same
    // reason.

    // Group the passed-in (section, (option, value)) tuples by section name.
    val t1: Map[String, Seq[(String, (String, String))]] = values.groupBy(_._1)

    // Map t1 so that we:
    //
    // (a) drop the section name from each map value (so that each map value
    //     is an (option, value) pair, and
    // (b) map the "value" part of (option, value) from a String to a Value.
    //
    // Then, we'll end up with a new contents map we can merge with the
    // existing one.
    val t2: Map[String, Map[String, Value]] = t1.map { case (sect, entries) =>
      val optionsAndVals = for { (_, ov) <- entries
                                 (option, valueString) = ov }
                           yield (option, Value(valueString))

      (sect, optionsAndVals.toMap)
    }

    // Finally, merge the two maps.
    val newContents = t2.map { case (sectionName, optionsMap) =>
      val optExistingOptions = contents.get(sectionName)

      // If there's an existing map, add the new map to the existing one.
      // Otherwise, just use the new one.
      val newOptionsMap = optExistingOptions.map { existingOptionsMap =>
        existingOptionsMap ++ optionsMap
      }
      .getOrElse(optionsMap)

      (sectionName, newOptionsMap)
    }

    // Finally, construct the new Configuration.
    new Configuration(contents            = newContents,
                      sectionNamePattern  = this.sectionNamePattern,
                      commentPattern      = this.commentPattern,
                      normalizeOptionName = this.normalizeOptionName,
                      notFoundFunction    = this.notFoundFunction,
                      safe                = this.safe)
  }

  /** Add new sections to the configuration. Example usage:
    *
    * {{{
    *   val cfg = Configuration(...)
    *   val newCfg = cfg ++ Map(
    *     "newSection1" -> Map("option1" -> "value1",
    *                          "option2" -> "value2"),
    *     "newSection2" -> Map("option1" -> "value1")
    *   )
    * }}}
    *
    * @param newValues A map of (section -> Map(option -> value)) values
    *
    * @return new configuration
    */
  def ++(newValues: Map[String, Map[String, String]]): Configuration = {

    val sequence = for { (sectionName, optionsMap) <- newValues.toSeq
                         optionValue               <- optionsMap.toSeq }
                   yield (sectionName, optionValue)

    ++(sequence: _*)
  }

  /** Remove a value from the configuration, returning a new object. If the
    * section or option don't exist, the original configuration is returned
    * (not a copy). If the section and option exist, the option is removed.
    * If the section is then empty, it's also removed.
    *
    * @param section  the section name
    * @param option   the option name
    *
    * @return a new `Configuration` object with the change applied, or the
    *         original configuration if the section or option weren't
    *         there.
    */
  def -(section: String, option: String): Configuration = {
    val optNewContents = for { sectionMap <- contents.get(section)
                               value      <- sectionMap.get(option) }
    yield {
      val newSection = sectionMap - option
      if (newSection.isEmpty)
        contents - section
      else
        contents + (section -> newSection)
    }

    optNewContents.map { newContents =>
      new Configuration(contents            = newContents,
                        sectionNamePattern  = this.sectionNamePattern,
                        commentPattern      = this.commentPattern,
                        normalizeOptionName = this.normalizeOptionName,
                        notFoundFunction    = this.notFoundFunction,
                        safe                = this.safe)
    }.
    getOrElse(this)
  }

  /** Remove multiple (section -> option) pairs from the configuration,
    * returning the new configuration. Example use:
    *
    * {{{
    *   val cfg = Configuration(...)
    *   val newCfg = cfg -- (("newSection1" -> "option1"),
    *                        ("newSection2" -> "option1"),
    *                        ("newSection1" -> "option3"))
    * }}}
    *
    * @param values sequence of (section, option) pairs
    *
    * @return new configuration
    */
  def --(values: Seq[(String, String)]): Configuration = {
    // Group the passed-in (section, option) pairs by section name.
    val grouped: Map[String, Seq[(String, String)]] = values.groupBy(_._1)

    // Strip the section name from the grouped values.
    val groupedValuesMap = grouped.map { case (section, seq) =>
      (section, seq.map(_._2))
    }

    val emptyMapPlaceholder = Map.empty[String, Value]

    // Create a new content map by subtracting the options from existing
    // sections. Note that we may well end up with empty sections.
    val newContents1 = contents.map { case (sectionName, optionsMap) =>
      groupedValuesMap.get(sectionName).map { removeOptions =>
        sectionName -> (optionsMap -- removeOptions)
      }.
      getOrElse {
        sectionName -> optionsMap
      }
    }

    // Now, remove empty sections.
    val newContents2 = newContents1.filter { case (section, optionsMap) =>
      optionsMap.nonEmpty
    }

    if (contents == newContents2) {
      this
    }
    else {
      // Finally, construct the new Configuration.
      new Configuration(contents            = newContents2,
                        sectionNamePattern  = this.sectionNamePattern,
                        commentPattern      = this.commentPattern,
                        normalizeOptionName = this.normalizeOptionName,
                        notFoundFunction    = this.notFoundFunction,
                        safe                = this.safe)
    }
  }

  /** Determine whether the configuration contains a named section.
    *
    * @param sectionName  the new section's name
    *
    * @return `true` if the configuration has a section with that name,
    *         `false` otherwise
    */
  def hasSection(sectionName: String): Boolean =
    sections contains sectionName

  /** Get all options in a section.
    *
    * @param sectionName  the section name
    *
    * @return a map of all options and their values for the section. If
    *         the section doesn't exist, an empty map is returned.
    */
  def options(sectionName: String): Map[String, String] = {
    getSection(sectionName).map { section =>
      section.options
    }.
    getOrElse(Map.empty[String, String])
  }

  /** Get the list of option names.
    *
    * @param sectionName the section's name
    *
    * @return a list of option names in that section. The iterator will be
    *         empty if the section doesn't exist.
    */
  def optionNames(sectionName: String): Iterator[String] = {
    getSection(sectionName).map { section =>
      section.options.keys.iterator
    }.
    getOrElse(Seq.empty[String].iterator)
  }

  /** Invoke a code block on each section whose name matches a regular
    * expression.
    *
    * @param regex  the regular expression to match
    * @param code   the block of code to invoke with each section
    */
  def forMatchingSections(regex: Regex)(code: Section => Unit) = {
    for (name <- sectionNames; if (regex.findFirstIn(name) != None))
      code(new Section(name, options(name)))
  }

  /** Return a sequence of sections whose name match matches a regular
    * expression.
    *
    * @param regex  the regular expression to match
    */
  def matchingSections(regex: Regex): Seq[Section] = {
    sectionNames.filter { name => regex.findFirstIn(name) != None }
                .map { name => new Section(name, options(name)) }
                .toSeq
  }

  // --------------------------------------------------------------------------
  // Private methods
  // --------------------------------------------------------------------------

  private def resolveOpt(sectionName: String, value: String): Option[String] = {
    resolveEither(sectionName, value) match {
      case Left(e)    => None
      case Right(opt) => opt
    }
  }

  private def resolveEither(sectionName: String, value: String):
    Either[String, Option[String]] = {

    val template = new UnixShellStringTemplate(templateResolve(sectionName, _),
                                               "[a-zA-Z0-9_.]+",
                                               safe)
    template.sub(value) match {
      case Right(s) => Right(Some(s))
      case Left(e)  => Left(s"Can't get '$value' from $sectionName: $e")
    }
  }

  private def templateResolve(sectionName:  String,
                              variableName: String): Option[String] = {
    variableName match {
      case FullVariableRef(section, option) => rawValue(section, option)
      case VariableName(option)             => rawValue(sectionName, option)
      case _                                => None
    }
  }

  private def rawValue(sectionName: String, optionName: String): Option[String] = {
    sectionName match {
      case "env" => Option(System.getenv(optionName))
      case "system" => Option(System.getProperties.getProperty(optionName))
      case _ if (!hasSection(sectionName)) => None
      case _ => {
        val key = OptionKey(optionName)
        sections(sectionName).get(key).flatMap { value =>
          resolveOpt(sectionName, value.value)
        }
      }
    }
  }
}

/**
  * Companion object for the `Configuration` class
  */
object Configuration {
  import java.io.File

  final val DefaultSectionNamePattern = """([a-zA-Z0-9_]+)""".r
  final val DefaultCommentPattern     = """^\s*(#.*)$""".r

  private val SpecialSections  = Set("env", "system")

  private def DefaultOptionNameTransformer(name: String) = name.toLowerCase()

  /** Read a configuration file, returning an `Either`, instead of throwing
    * an exception on error.
    *
    * @param source              `scala.io.Source` object to read
    * @param sectionNamePattern  Regular expression that matches legal section
    *                            names. Defaults as described above.
    * @param commentPattern      Regular expression that matches comment lines.
    *                            Default: ^\s*(#.*)$
    * @param normalizeOptionName Partial function used to transform option names
    *                            into keys. The default function transforms
    *                            the names to lower case.
    * @param notFoundFunction    a function to call if an option isn't found in
    *                            the configuration, or None. The function
    *                            must take a section name and an option name as
    *                            parameters. It must return `Left(error)` on
    *                            error, `Right(None)` if the value isn't found,
    *                            and `Right(string)` if the value is found.
    * @param safe                `true` does "safe" substitutions, with
    *                            substitutions of nonexistent values replaced by
    *                            empty strings. `false` ensures that bad
    *                            substitutions result in errors (or `None` in
    *                            functions, like `get()`, that return `Option`
    *                            values).
    *
    *@return `Right(config)` on success, `Left(error)` on error.
    */
  def apply(source:              Source,
            sectionNamePattern:  Regex = Configuration.DefaultSectionNamePattern,
            commentPattern:      Regex = Configuration.DefaultCommentPattern,
            normalizeOptionName: (String => String) = DefaultOptionNameTransformer,
            notFoundFunction:    Option[Types.NotFoundFunction] = None,
            safe:                Boolean = true):
    Either[String, Configuration] = {

    load(source, sectionNamePattern, commentPattern).map { map =>
      new Configuration(map,
                        sectionNamePattern,
                        commentPattern,
                        normalizeOptionName,
                        notFoundFunction,
                        safe)
    }
  }

  /** Read a configuration file, permitting some predefined sections to be
    * added to the configuration before it is read. The predefined sections
    * are defined in a map of maps. The outer map is keyed by predefined
    * section name. The inner maps consist of options and their values.
    * For instance, to read a configuration file, giving it access to
    * certain command line parameters, you could do something like this:
    *
    * {{{
    * object Foo {
    *   def main(args: Array[String]) = {
    *     // You'd obviously want to do some real argument checking here.
    *     val configFile = args(0)
    *     val name = args(1)
    *     val ipAddress = args(2)
    *     val sections = Map("args" -> Map("name" -> name, "ip" -> ipAddress))
    *     val config = Configuration(Source.fromFile(new File(configFile)), sections)
    *     ...
    *   }
    * }
    * }}}
    *
    * @param source              `scala.io.Source` object to read
    * @param sections            the predefined sections. An empty map means
    *                            there are no predefined sections.
    *
    * @return `Right[Configuration]` on success, `Left(error)` on error.
    */
  def apply(source: Source, sections: Map[String, Map[String, String]]):
    Either[String, Configuration] = {

    apply(source, sections)
  }

  /** Read a configuration file, permitting some predefined sections to be
    * added to the configuration before it is read. The predefined sections
    * are defined in a map of maps. The outer map is keyed by predefined
    * section name. The inner maps consist of options and their values.
    * For instance, to read a configuration file, giving it access to
    * certain command line parameters, you could do something like this:
    *
    * {{{
    * object Foo {
    *   def main(args: Array[String]) = {
    *     // You'd obviously want to do some real argument checking here.
    *     val configFile = args(0)
    *     val name = args(1)
    *     val ipAddress = args(2)
    *     val sections = Map("args" -> Map("name" -> name, "ip" -> ipAddress))
    *     val config = Configuration(Source.fromFile(new File(configFile)), sections)
    *     ...
    *   }
    * }
    * }}}
    *
    * @param source              `scala.io.Source` object to read
    * @param sections            the predefined sections. An empty map means
    *                            there are no predefined sections.
    *                            not (`true`). Default: `false`
    * @param sectionNamePattern  Regular expression that matches legal section
    *                            names.
    * @param commentPattern      Regular expression that matches comment lines.
    *
    * @return `Right(config)` on success, `Left(error)` on error.
    */
  def apply(source: Source,
            sections: Map[String, Map[String, String]],
            sectionNamePattern: Regex,
            commentPattern: Regex):
    Either[String, Configuration] = {

    apply(source, sections, sectionNamePattern, commentPattern)
  }

  // --------------------------------------------------------------------------
  // Objects
  // --------------------------------------------------------------------------

  /** Import this object's contents (`import Configuration.Implicits._`)
    * to get the implicit converters.
    */
  object Implicits {

    /** Value converter for Boolean values, for use with
      * `Configuration.asEither()` and `Configuration.asOpt()`.
      */
    implicit object BooleanValueConverter extends ValueConverter[Boolean] {
      def convert(sectionName: String,
                  optionName:  String,
                  value:       String): Either[String, Boolean] = {
        import grizzled.string.util._

        strToBoolean(value) match {
          case Left(error) => {
            Left(s"Section '$sectionName', option '$optionName': '$value' is " +
                 "not boolean: $error")
          }

          case Right(b) => Right(b)
        }
      }
    }

    /** Value converter for integer values, for use with
      * `Configuration.asEither()` and `Configuration.asOpt()`.
      */
    implicit object IntConverter extends ValueConverter[Int] {
      def convert(sectionName: String,
                  optionName:  String,
                  value:       String): Either[String, Int] = {
        Try { Integer.parseInt(value) } match {
          case Failure(e) => {
            Left(s"Section '$sectionName', option '$optionName': '$value' is " +
              "not an integer.")
          }

          case Success(i) => Right(i)
        }
      }
    }

    /** Value converter for long integer values, for use with
      * `Configuration.asEither()` and `Configuration.asOpt()`.
      */
    implicit object LongConverter extends ValueConverter[Long] {
      def convert(sectionName: String,
                  optionName:  String,
                  value:       String): Either[String, Long] = {
        Try { java.lang.Long.parseLong(value) } match {
          case Failure(e) => {
            Left(s"Section '$sectionName', option '$optionName': '$value' is " +
              "not an integer.")
          }

          case Success(i) => Right(i)
        }
      }
    }

    /** Value converter for String values, for use with
      * `Configuration.asEither()` and `Configuration.asOpt()`.
      */
    implicit object StringConverter extends ValueConverter[String] {
      def convert(sectionName: String,
                  optionName:  String,
                  value:       String): Either[String, String] = {
        Right(value)
      }
    }

    /** Value converter for Character values, for use with
      * `Configuration.asEither()` and `Configuration.asOpt()`.
      */
    implicit object CharConverter extends ValueConverter[Character] {
      def convert(sectionName: String,
                  optionName:  String,
                  value:       String): Either[String, Character] = {
        if (value.length == 1)
          Right(value(0))
        else
          Left(s"Section '$sectionName', option '$optionName': '$value' is " +
               "not a character.")
      }
    }
  }

  // --------------------------------------------------------------------------
  // Private methods
  // --------------------------------------------------------------------------

  /** Map a user-supplied section map into an internal one.
    *
    * @param sectionMap A map of section names to options
    *
    * @return the internal version of the same map
    */
  private def mapSectionMap(sectionMap: Map[String, Map[String, String]]):
    Map[String, Map[String, Value]] = {

    sectionMap.map { case (sectionName: String, values: Map[String, String]) =>
      sectionName -> values.map { case (k, v) => k -> Value(v, true) }
    }
  }

  /** Load configuration data from the specified source into this object.
    * Clears the configuration first.
    *
    * @param source  `scala.io.Source` object to read
    *
    * @return this object, for convenience
    */
  private def load(
    source:             Source,
    sectionNamePattern: Regex = Configuration.DefaultSectionNamePattern,
    commentPattern:     Regex = Configuration.DefaultCommentPattern
  ): Either[String, Map[String, Map[String, Value]]] = {

    val SectionName      = sectionNamePattern
    val SectionNameString= SectionName.toString
    val ValidSection     = ("""^\s*\[""" + SectionNameString + """\]\s*$""").r
    val BadSectionFormat = """^\s*(\[[^\]]*)$""".r
    val BadSectionName   = """^\s*\[(.*)\]\s*$""".r
    val CommentLine      = commentPattern
    val BlankLine        = """^(\s*)$""".r
    val VariableNameString = """([a-zA-Z0-9_.]+)"""
    val RawAssignment    = ("""^\s*""" + VariableNameString + """\s*->\s*(.*)$""").r
    val Assignment       = ("""^\s*""" + VariableNameString + """\s*[:=]\s*(.*)$""").r


    def processLine(line: String,
                    curSection: Option[String],
                    curMap: Map[String, Map[String, Value]]):
      Either[String, (Option[String], Map[String, Map[String, Value]])] = {

      line match {
        case CommentLine(_) => Right((curSection, curMap))

        case BlankLine(_) => Right((curSection, curMap))

        case ValidSection(name) => {
          val newMap = curMap ++ Map(name -> Map.empty[String, Value])
          Right((Some(name), newMap))
        }

        case BadSectionFormat(section) =>
          Left(s"Badly formatted section: '$section'.")

        case BadSectionName(name) =>
          Left(s"Bad section name: '$name'.")

        case Assignment(optionName, value) => {
          curSection.map { sectionName =>
            val sectionMap = curMap.getOrElse(sectionName, Map.empty[String, Value])
            val newSection = sectionMap + (optionName -> Value(value))
            Right((curSection, curMap ++ Map(sectionName -> newSection)))
          }.
          getOrElse(
            Left(s"Assignment '$optionName=$value' occurs before the first " +
                 "section")
          )
        }

        case RawAssignment(optionName, value) => {
          curSection.map { sectionName =>
            val sectionMap = curMap.getOrElse(sectionName, Map.empty[String, Value])
            val newSection = sectionMap + (optionName -> Value(value, true))
            val newMap = curMap + (sectionName -> newSection)
            Right((curSection, newMap))
          }.
          getOrElse(
              Left(s"Assignment '$optionName=$value' occurs before the first " +
                   "section")
          )
        }

        case _ =>
          Left(s"Unrecognized configuration line: '$line'")
      }
    }

    @tailrec def processLines(lines: Iterator[String],
                              curSection: Option[String],
                              curMap: Map[String, Map[String, Value]]):
      Either[String, Map[String, Map[String, Value]]] = {

      if (lines.hasNext) {
        processLine(lines.next, curSection, curMap) match {
          case Left(error) => Left(error)
          case Right((section, map)) => processLines(lines, section, map)
        }
      }
      else {
        Right(curMap)
      }
    }

    processLines(new BackslashContinuedLineIterator(Includer(source)), None,
                 Map.empty[String, Map[String, Value]])
  }

}

private[config] case class Value(value: String, isRaw: Boolean = false) {
  override val toString = s"Value<value=$value, isRaw=$isRaw>"
}

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
 * Classes and objects to aid in the parsing of INI-style configuration
 * files. This package is similar, in concept, to the Python
 * <tt>ConfigParser</tt> module (though its implementation and capabilities
 * differ quite a bit).
 */
package grizzled.config

import grizzled.file.Includer
import grizzled.file.filter.BackslashContinuedLineIterator
import grizzled.string.template.UnixShellStringTemplate
import grizzled.string.implicits._

import scala.collection.mutable.{Map => MutableMap}
import scala.io.Source

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

/**
 * An INI-style configuration file parser.
 *
 * <p><tt>Configuration</tt> implements an in-memory store for a
 * configuration file whose syntax is reminiscent of classic Windows .INI
 * files, though with many extensions.</p>
 *
 * <h3>Syntax</h3>
 *
 * <p>A configuration file is broken into sections, and each section is
 * introduced by a section name in brackets. For example:</p>
 *
 * <blockquote><pre>
 * [main]
 * installation.directory=/usr/local/foo
 * program.directory: /usr/local/foo/programs
 *
 * [search]
 * searchCommand: find /usr/local/foo -type f -name '*.class'
 *
 * [display]
 * searchFailedMessage=Search failed, sorry.
 * </pre></blockquote>
 *
 * <p>Notes and caveats:</p>
 *
 * <ul>
 *   <li> At least one section is required.
 *   <li> Sections may be empty.
 *   <li> It is an error to have any variable definitions before the first
 *        section header.
 *   <li> The section names "system" and "env" are reserved. They don't really
 *        exist, but they're used during variable substitution (see below)
 *        to substitute from <tt>System.properties</tt> and the environment,
 *        respectively.
 * </ul>
 *
 * <h4>Section Name Syntax</h4>
 *
 * <p>There can be any amount of whitespace before and after the brackets
 * in a section name; the whitespace is ignored. Section names may consist
 * of alphanumeric characters and underscores. Anything else is not
 * permitted.</p>
 *
 * <h4>Variable Syntax</h4>
 *
 * <p>Each section contains zero or more variable settings. Similar to a
 * Java <tt>Properties</tt> file, the variables are specified as name/value
 * pairs, separated by an equal sign ("=") or a colon (":"). Variable names
 * are case-sensitive by default, though the case-sensitivity (and other
 * aspects of the variable name) may be changed by subclassing
 * <tt>Configuration</tt> and providing your own version of the
 * <tt>transformOptionName()</tt> method. Variable names may contain
 * alphanumerics, underscores, and hyphens (-).
 * Variable values may contain
 * anything at all. The parser ignores whitespace on either side of the "="
 * or ":"; that is, leading whitespace in the value is skipped. The way to
 * include leading whitespace in a value is escape the whitespace
 * characters with backslashes. (See below).</p>
 *
 * <h4>Continuation Lines</h4>
 *
 * <p>Variable definitions may span multiple lines; each line to be
 * continued must end with a backslash ("\") character, which escapes the
 * meaning of the newline, causing it to be treated like a space character.
 * The following line is treated as a logical continuation of the first
 * line. Unlike Java properties files, however, leading whitespace is
 * <i>not</i> removed from continued lines.</p>
 *
 * <p>Only variable definition lines may be continued. Section header
 * lines, comment lines (see below) and include directives (see below)
 * cannot span multiple lines.</p>
 *
 * <h4>Expansions of Variable Values</h4>
 *
 * <p>The configuration parser preprocesses each variable's value,
 * replacing embedded metacharacter sequences and substituting variable
 * references. You can use backslashes to escape the special characters
 * that the parser uses to recognize metacharacter and variable sequences;
 * you can also use single quotes. See <a href="#RawValues">Suppressing
 * Metacharacter Expansion and Variable Substitution</a>, below, for more
 * details.</p>
 *
 * <h5>Metacharacters</h5>
 *
 * <p>The parser recognizes Java-style ASCII escape sequences <tt>\t</tt>,
 * <tt>\n</tt>, <tt>\r</tt>, <tt>\\</tt>, <tt>\&nbsp;</tt> (a backslash and
 * a space), and <tt>&#92;u</tt><i>xxxx</i> are recognized and converted to
 * single characters. Note that metacharacter expansion is performed
 * <i>before</i> variable substitution.</p>
 *
 * <h5>Variable Substitution</h5>
 *
 * <p>A variable value can interpolate the values of other variables, using
 * a variable substitution syntax. The general form of a variable reference
 * is <tt>${sectionName.varName}</tt>.</p>
 *
 * <ul>
 *   <li><tt>sectionName</tt> is the name of the section containing the
 *       variable to substitute; if omitted, it defaults to the current
 *       section.
 *   <li><tt>varName</tt> is the name of the variable to substitute.
 * </ul>
 *
 * <p>If a variable reference specifies a section name, the referenced section
 * must precede the current section. It is not possible to substitute the value
 * of a variable in a section that occurs later in the file.</p>
 *
 * <p>The section names "system" and "env" are reserved for special
 * "pseudosections."</p>
 *
 * <p>The "system" pseudosection is used to interpolate values from
 * <tt>System.properties</tt> For instance, <tt>${system.user.home}</tt>
 * substitutes the value of the <tt>user.home</tt> system property
 * (typically, the home directory of the user running the program).
 * Similarly, <tt>${system.user.name}</tt> substitutes the user's name.</p>
 *
 * <p>The "env" pseudosection is used to interpolate values from the
 * environment. On UNIX systems, for instance, <tt>${env.HOME}</tt>
 * substitutes user's home directory (and is, therefore, a synonym for
 * <tt>${system.user.home}</tt>. On some versions of Windows,
 * <tt>${env.USERNAME}</tt> will substitute the name of the user running
 * the program. Note: On UNIX systems, environment variable names are
 * typically case-sensitive; for instance, <tt>${env.USER}</tt> and
 * <tt>${env.user}</tt> refer to different environment variables. On
 * Windows systems, environment variable names are typically
 * case-insensitive; <tt>${env.USERNAME}</tt> and <tt>${env.username}</tt>
 * are equivalent.</p>
 *
 * <p>Notes and caveats:</p>
 *
 * <ul>
 *   <li> <tt>Configuration</tt> uses the
 *        <tt>grizzled.string.template.UnixShellVariableSubstituter</tt>
 *        class to do variable substitution, so it honors all the syntax
 *        conventions supported by that class.
 *
 *   <li> Variable substitutions are only permitted within variable
 *        values. They are ignored in variable names, section names,
 *        include directives and comments.
 *
 *   <li> Variable substitution is performed <i>after</i> metacharacter
 *        expansion (so don't include metacharacter sequences in your variable
 *        names).
 *
 *   <li> To include a literal "$" character in a variable value, escape
 *        it with a backslash, e.g., "<tt>var=value with \$ dollar sign</tt>"
 * </ul>
 *
 * <h5><a name="RawValues">Suppressing Metacharacter Expansion and Variable
 * Substitution</a></h5>
 *
 * <p>To prevent the parser from interpreting metacharacter sequences,
 * variable substitutions and other special characters, use the "->"
 * assignment operator, instead of ":" or "=".</p>
 *
 * <p>For example, suppose you want to set variable "prompt" to the
 * literal value "Enter value. To specify a newline, use \n." The following
 * configuration file line will do the trick:</p>
 *
 * <blockquote><pre>prompt -> Enter value. To specify a newline, use \n
 * </pre></blockquote>
 *
 * <p>Similarly, to set variable "abc" to the literal string "${foo}"
 * suppressing the parser's attempts to expand "${foo}" as a variable
 * reference, you could use:</p>
 *
 * <blockquote><pre>abc -> ${foo}</pre></blockquote>
 *
 * <p>Note: It's also possible, though hairy, to escape the special meaning
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
 * <tt>grizzled.string.template.UnixShellStringTemplate</tt> class
 * <tt>grizzled.GrizzledString.translateMetachars()</tt> method to do the
 * variable substitution and metacharacter translation. In general, you're
 * better off just sticking with the "->" assignment operator.</p>
 *
 * <h4>Includes</h4>
 *
 * <p>A special include directive permits inline inclusion of another
 * configuration file. The include directive takes two forms:
 *
 * <blockquote><pre>
 * %include "path"
 * %include "URL"
 * </pre></blockquote>
 *
 * <p>For example:</p>
 *
 * <blockquote><pre>
 * %include "/home/bmc/mytools/common.cfg"
 * %include "http://configs.example.com/mytools/common.cfg"
 * </pre></blockquote>
 *
 * <p>If the include path is not a URL, and is not an absolute path, its
 * location is relative to the file that's trying to include it.</p>
 * 
 * <p>The included file may contain any content that is valid for this
 * parser. It may contain just variable definitions (i.e., the contents of
 * a section, without the section header), or it may contain a complete
 * configuration file, with individual sections. Since
 * <tt>Configuration</tt> recognizes a variable syntax that is
 * essentially identical to Java's properties file syntax, it's also legal
 * to include a properties file, provided it's included within a valid
 * section.</p>
 *
 * <p>Note: Attempting to include a file from itself, either directly or
 * indirectly, will cause the parser to throw an exception.</p>
 *
 * <h4>Comments and Blank Lines</h4>
 *
 * <p>A comment line is a one whose first non-whitespace character is a "#".
 * A blank line is a line containing no content, or one containing only
 * white space. Blank lines and comments are ignored.</p>
 *
 * <h4>Caller-supplied Predefined Sections</h4>
 *
 * <p>Calling applications may supply predefined sections and options, in
 * the form of a map. These sections may then be used by other sections,
 * via variable references. The predefined sections are defined in a map of
 * maps. The outer map is keyed by predefined section name. The inner maps
 * consist of options and their values. For instance, to read a
 * configuration file, giving it access to certain command line parameters,
 * you could do something like this:</p>
 *
 * <blockquote><pre>
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
 * </pre></blockquote>
 *
 * @param predefinedSections  the predefined sections. An empty map means
 *                            there are no predefined sections.
 */
class Configuration(predefinedSections: Map[String, Map[String, String]])
{
    private val SpecialSections = Set("env", "system")

    private val sections = MutableMap.empty[String, MutableMap[String, String]]

    for ((sectionName, optionMap) <- predefinedSections)
    {
        addSection(sectionName)
        for ((optionName, optionValue) <- optionMap)
            addOption(sectionName, optionName, optionValue)
    }

    /**
     * Alternate constructor for use when there are no predefined sections.
     */
    def this() = this(Map.empty[String, Map[String, String]])

    /**
     * Get the list of section names.
     *
     * @return the section names, in a iterator
     */
    def sectionNames: Iterator[String] = sections.keys

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
    def addOption(sectionName: String, optionName: String, value: String)
    {
        if (SpecialSections contains sectionName)
            throw new ConfigException("Can't add an option to read-only " +
                                      "section \"" + sectionName + "\"")

        if (! hasSection(sectionName))
            throw new NoSuchSectionException(sectionName)

        val optionMap = sections(sectionName)
        val canonicalOptionName = transformOptionName(optionName)
        if (optionMap.contains(canonicalOptionName))
            throw new DuplicateOptionException(sectionName, optionName)

        optionMap += (canonicalOptionName -> value)
    }

    /**
     * Determine whether the configuration contains a named section.
     *
     * @param sectionName  the new section's name
     *
     * @return <tt>true</tt> if the configuration has a section with that name,
     *         <tt>false</tt> otherwise
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
     */
    def option(sectionName: String, optionName: String): String =
    {
        sectionName match
        {
            case "env" =>
                val option = System.getenv(optionName)
                if (option == null)
                    throw new NoSuchOptionException(sectionName, optionName)
                option

            case "system" =>
                val option = System.getProperties.getProperty(optionName)
                if (option == null)
                    throw new NoSuchOptionException(sectionName, optionName)
                option

            case _ =>
                if (! hasSection(sectionName))
                    throw new NoSuchSectionException(sectionName)

                val options = sections(sectionName)
                val canonicalOptionName = transformOptionName(optionName)
                if (! options.contains(canonicalOptionName))
                    throw new NoSuchOptionException(sectionName, optionName)
                options(canonicalOptionName)
        }
    }

    /**
     * Get the value for an option in a section, supplying a default if the
     * option or the section doesn't exist.
     *
     * @param sectionName  the section name
     * @param optionName   the option name
     * @param default      default value
     *
     * @return the option's value (which may be the default)
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
     * @return a map of all options and their values for the section
     *
     * @throws NoSuchSectionException if the section doesn't exist
     */
    def options(sectionName: String): Map[String, String] =
    {
        if (! hasSection(sectionName))
            throw new NoSuchSectionException(sectionName)

        Map.empty[String, String] ++ sections(sectionName)
    }

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

        sections(sectionName).keys
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
}

/**
 * A configuration reader: Reads a source and produces a parsed
 * configuration.
 */
object ConfigurationReader
{
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
    /**
     * Read a configuration.
     *
     * @param source <tt>scala.io.Source</tt> object to read
     *
     * @return the <tt>Configuration</tt> object.
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
     * <blockquote><pre>
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
     * </pre></blockquote>
     *
     * @param source    <tt>scala.io.Source</tt> object to read
     * @param sections  the predefined sections. An empty map means there are
     *                  no predefined sections.
     *
     * @return the <tt>Configuration</tt> object.
     */
    def read(source: Source,
             sections: Map[String, Map[String, String]]): Configuration =
    {
        val config             =  new Configuration(sections)
        var curSection: String = null

        def resolveVariable(varName: String): Option[String] =
            getVar(config, curSection, varName)

        val template = new UnixShellStringTemplate(resolveVariable,
                                                   "[a-zA-Z0-9_.]+",
                                                   true)

        for (line <- new BackslashContinuedLineIterator(Includer(source)))
        {
            line match
            {
                case CommentLine(_) =>

                case BlankLine(_) =>

                case ValidSection(name) =>
                    config.addSection(name)
                    curSection = name

                case BadSectionFormat(section) =>
                    throw new ConfigException("Badly formatted section: \"" +
                                              section + "\"")

                case BadSectionName(name) =>
                    throw new ConfigException("Bad section name: \"%s\"" + name)

                case Assignment(optionName, value) =>
                    if (curSection == null)
                        throw new ConfigException("Assignment \"" +
                                                  optionName + "=" + value +
                                                  "\" occurs before the " +
                                                  "first section.")
                    val newValue = template.substitute(value.translateMetachars)
                    config.addOption(curSection, optionName, newValue)

                case RawAssignment(optionName, value) =>
                    if (curSection == null)
                        throw new ConfigException("Assignment \"" +
                                                  optionName + "=" + value +
                                                  "\" occurs before the " +
                                                  "first section.")
                    config.addOption(curSection, optionName, value)

                case _ =>
                    throw new ConfigException("Unknown configuration line: \"" +
                                              line + "\"")
            }
        }

        config
    }

    private def getVar(config: Configuration,
                       curSection: String,
                       varName: String): Option[String] =
    {
        try
        {
            varName match
            {
                case FullVariableRef(section, option) =>
                    Some(config.option(section, option))
                case VariableName(option) =>
                    Some(config.option(curSection, option))
                case _ =>
                    throw new ConfigException("Reference to nonexistent " +
                                              "section or option: ${ " +
                                              varName + "}")
            }
        }

        catch
        {
            case _: NoSuchOptionException =>
                throw new ConfigException("In section [" + curSection +
                                          "]: Reference to nonexistent " +
                                          "option in ${" + varName + "}")

            case _: NoSuchSectionException =>
                throw new ConfigException("In section [" + curSection +
                                          "]: Reference to nonexistent " +
                                          "option in ${" + varName + "}")
        }
    }
}

/**
 * Companion object for the <tt>Configuration</tt> class
 */
object Configuration
{
    import java.io.File

    /**
     * Read a configuration file.
     *
     * @param source <tt>scala.io.Source</tt> object to read
     *
     * @return the <tt>Configuration</tt> object.
     */
    def apply(source: Source): Configuration = ConfigurationReader.read(source)

    /**
     * Read a configuration file, permitting some predefined sections to be
     * added to the configuration before it is read. The predefined sections
     * are defined in a map of maps. The outer map is keyed by predefined
     * section name. The inner maps consist of options and their values.
     * For instance, to read a configuration file, giving it access to
     * certain command line parameters, you could do something like this:
     *
     * <blockquote><pre>
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
     * </pre></blockquote>
     *
     * @param source    <tt>scala.io.Source</tt> object to read
     * @param sections  the predefined sections. An empty map means there are
     *                  no predefined sections.
     *
     * @return the <tt>Configuration</tt> object.
     */
    def apply(source: Source,
              sections: Map[String, Map[String, String]]): Configuration =
        ConfigurationReader.read(source, sections)
}

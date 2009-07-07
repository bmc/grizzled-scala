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
class NoSuchSectionException(message: String)
    extends ConfigException(message)

/**
 * Thrown when an expected option is not present in the confiruation.
 */
class NoSuchOptionException(message: String)
    extends ConfigException(message)

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
 * of alphanumeric characters and periods. Anything else is not
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
 * line; however, any leading whitespace is removed from continued lines.
 * For example, the following four variable assignments all have the
 * same value:
 *
 * <blockquote><pre>
 * [test]
 * a: one two three
 * b:            one two three
 * c: one two \
 * three
 * d:        one \
 *                         two \
 *    three
 * </pre></blockquote>
 *
 * <p>Because leading whitespace is skipped, all four variables have the
 * value "one two three".</p>
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
 * <tt>\n</tt>, <tt>\r</tt>, <tt>\\</tt>, <tt>\"</tt>, <tt>\'</tt>,
 * <tt>\&nbsp;</tt> (a backslash and a space), and
 * <tt>&#92;u</tt><i>xxxx</i> are recognized and converted to single
 * characters. Note that metacharacter expansion is performed <i>before</i>
 * variable substitution.</p>
 *
 * <h5>Variable Substitution</h5>
 *
 * <p>A variable value can interpolate the values of other variables, using
 * a variable substitution syntax. The general form of a variable reference
 * is <tt>${sectionName:varName?default}</tt>.</p>
 *
 * <ul>
 *   <li><tt>sectionName</tt> is the name of the section containing the
 *       variable to substitute; if omitted, it defaults to the current
 *       section.
 *   <li><tt>varName</tt> is the name of the variable to substitute.
 *   <li><tt>default</tt> is the default value for the variable, if the
 *       variable is undefined. If omitted, a reference to an undefined
 *       variable (or undefined section) will either result in an exception
 *       or will be replaced with  an empty string, depending on the setting
 *       of the "abort on undefined value" flag. See
 *       {@link #setAbortOnUndefinedVariable}.
 * </ul>
 *
 * <p>If a variable reference specifies a section name, the referenced section
 * must precede the current section. It is not possible to substitute the value
 * of a variable in a section that occurs later in the file.</p>
 *
 * <p>The section names "system", "env", and "program" are reserved for
 * special "pseudosections."</p>
 *
 * <p>The "system" pseudosection is used to interpolate values from Java's
 * <tt>System.properties</tt> class. For instance,
 * <tt>${system:user.home}</tt> substitutes the value of the
 * <tt>user.home</tt> system property (typically, the home directory of the
 * user running <i>curn</i>). Similarly, <tt>${system:user.name}</tt>
 * substitutes the user's name.</p>
 *
 * <p>For example:</p>
 *
 * <blockquote><pre>
 * [main]
 * installation.directory=${system:user.home?/tmp}/this_package
 * program.directory: ${installation.directory}/foo/programs
 *
 * [search]
 * searchCommand: find ${main:installation.directory} -type f -name '*.class'
 *
 * [display]
 * searchFailedMessage=Search failed, sorry.
 * </pre></blockquote>
 *
 * <p>The "env" pseudosection is used to interpolate values from the
 * environment. On UNIX systems, for instance, <tt>${env:HOME}</tt>
 * substitutes user's home directory (and is, therefore, a synonym for
 * <tt>${system:user.home}</tt>. On some versions of Windows,
 * <tt>${env:USERNAME}</tt> will substitute the name of the user running
 * <i>curn</i>. Note: On UNIX systems, environment variable names are
 * typically case-sensitive; for instance, <tt>${env:USER}</tt> and
 * <tt>${env:user}</tt> refer to different environment variables. On
 * Windows systems, environment variable names are typically
 * case-insensitive; <tt>${env:USERNAME}</tt> and <tt>${env:username}</tt>
 * are equivalent.</p>
 *
 * <p>The "program" pseudosection is a placeholder for various special
 * variables provided by the <tt>Configuration</tt> class. Those variables
 * are:</p>
 *
 * <table border="1" align="left" width="100%" class="nested-table">
 *   <tr valign="top">
 *     <th align="left">Variable</th>
 *     <th align="left">Description</th>
 *     <th align="left">Examples</th>
 *   </tr>
 *
 *   <tr valign="top">
 *     <td align="left"><tt>cwd</tt></td>
 *     <td align="left">
 *        the program's current working directory. Thus,
 *        <tt>${program:cwd}</tt> will substitute the working directory,
 *        with the appropriate system-specific file separator. On a Windows
 *        system, the file separator character (a backslash) will be doubled,
 *        to ensure that it is properly interpreted by the configuration file
 *        parsing logic.
 *     </td>
 *     <td align="left">&nbsp;</td>
 *   </tr>
 *
 *   <tr valign="top">
 *     <td align="left"><tt>cwd.url</tt></td>
 *     <td align="left">
 *        the program's current working directory as a <tt>file</tt> URL,
 *        without the trailing "/". Useful when you need to create a URL
 *        reference to something relative to the current directory. This is
 *        especially useful on Windows, where
 *        <blockquote><pre>file://${program:cwd}/something.txt</pre></blockquote>
 *         produces an invalid URL, with a mixture of backslashes and
 *         forward slashes.  By contrast,
 *         <blockquote><pre>${program:cwd.url}/something.txt</pre></blockquote>
 *         always produces a valid URL, regardless of the underlying host
 *         operating system.
 *     </td>
 *     <td align="left">&nbsp;</td>
 *   </tr>
 *
 *   <tr valign="top">
 *     <td align="left"><tt>now</tt></td>
 *     <td align="left">
 *        the current time, formatted by calling
 *        <tt>java.util.Date.toString()</tt> with the default locale.
 *     </td>
 *     <td align="left">&nbsp;</td>
 *   </tr>
 *
 *   <tr valign="top">
 *     <td align="left" nowrap>
 *       <tt>now</tt> <i>delim</i> <i>fmt</i> [<i>delim</i> <i>lang delim country</i>]]
 *     </td>
 *     <td align="left">
 *        <p>The current date/time, formatted with the specified
 *        <tt>java.text.SimpleDateFormat</tt> format string. If specified,
 *        the given locale and country code will be used; otherwise, the
 *        default system locale will be used. <i>lang</i> is a Java language
 *        code, such as "en", "fr", etc. <i>country</i> is a 2-letter country
 *        code, e.g., "UK", "US", "CA", etc. <i>delim</i> is a user-chosen
 *        delimiter that separates the variable name ("<tt>now</tt>") from the
 *        format and the optional locale fields. The delimiter can be anything
 *        that doesn't appear in the format string, the variable name, or
 *        the locale.</p>
 *
 *        <p>Note: <tt>SimpleDateFormat</tt> requires that literal strings
 *        (i.e., strings that should not be processed as part of the format)
 *        be enclosed in quotes. For instance:</p>
 *
 *        <blockquote><pre>yyyy.MM.dd 'at' hh:mm:ss z</pre></blockquote>
 *
 *        <p>Because single quotes are special characters in configuration
 *        files, it's important to escape them if you use them inside date
 *        formats. So, to include the above string in a configuration
 *        file's <tt>${program:now}</tt> reference, use the following:</p>
 *
 *        <blockquote><pre>${program:now/yyyy.MM.dd \'at\' hh:mm:ss z}</pre></blockquote>
 *
 *        <p>See <a href="#RawValues">Suppressing Metacharacter Expansion
 *        and Variable Substitution</a>, below, for more details.</p>
 *     </td>
 *     <td align="left">
 * <pre> ${program:now|yyyy.MM.dd 'at' hh:mm:ss z}
 * ${program:now|yyyy/MM/dd 'at' HH:mm:ss z|en|US}
 * ${program:now|dd MMM, yyyy hh:mm:ss z|fr|FR}</pre>
 *     </td>
 *   </tr>
 * </table>
 *
 * <p>Notes and caveats:</p>
 *
 * <ul>
 *   <li> <tt>Configuration</tt> uses the
 *        {@link UnixShellVariableSubstituter} class to do variable
 *        substitution, so it honors all the syntax conventions supported
 *        by that class.
 *
 *   <li> A variable that directly or indirectly references itself via
 *        variable substitution will cause the parser to throw an exception.
 *
 *   <li> Variable substitutions are only permitted within variable
 *        values and include targets (see below). They are ignored in variable
 *        names, section names, and comments.
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
 * variable substitutions and other special characters, enclose part or
 * all of the value in single quotes.
 * <p>For example, suppose you want to set variable "prompt" to the
 * literal value "Enter value. To specify a newline, use \n." The following
 * configuration file line will do the trick:</p>
 *
 * <blockquote><pre>prompt: 'Enter value. To specify a newline, use \n'
 * </pre></blockquote>
 *
 * <p>Similarly, to set variable "abc" to the literal string "${foo}"
 * suppressing the parser's attempts to expand "${foo}" as a variable
 * reference, you could use:</p>
 *
 * <blockquote><pre>abc: '${foo}'</pre></blockquote>
 *
 * <p>To include a literal single quote, you must escape it with a
 * backslash.</p>
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
 * currently do that, because I wanted to make use of the existing
 * {@link XStringBuffer#decodeMetacharacters()} method and the
 * {@link UnixShellVariableSubstituter} class. In general, you're better off
 * just sticking with single quotes.</p>
 *
 * <h5>Double Quotes</h5>
 *
 * <p>Double quotes can be used to escape the special meaning of white
 * space, while still permitting metacharacters and variable references to
 * be expanded. (Metacharacter and variable references are not expanded
 * between single quotes.) When retrieving a variable's value via
 * {@link #getConfigurationValue}, a program will not be able to tell whether
 * double quotes were used or not, since {@link #getConfigurationValue}
 * returns the "cooked" value as a single string. However, callers can use
 * the {@link #getConfigurationTokens} method to retrieve the parsed tokens
 * that comprise a configuration value. Double- and single-quoted strings are
 * returned as individual tokens.</p>
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
 * <p>A comment line is a one whose first non-whitespace character is a "#"
 * or a "!". This comment syntax is identical to the one supported by a
 * Java properties file. A blank line is a line containing no content, or
 * one containing only whitespace. Blank lines and comments are ignored.</p>
 *
 * @version <tt>$Revision$</tt>
 *
 * @author Copyright &copy; 2004-2007 Brian M. Clapper
 */
class Configuration(defaultValues: Map[String, String])
{
    // This complicated-looking transform maps the defaultValues map into a new
    // one with the keys all normalized by transformOptionName().
    val defaults = Map(
        (defaultValues.map((a) => (transformOptionName(a._1), a._2))).toSeq: _*
    )

    val sectionMap = MutableMap.empty[String, MutableMap[String, String]]

    def this() = this(Map.empty[String, String])

    /**
     * Get the list of section names.
     *
     * @return the section names, in a iterator
     */
    def sectionNames: Iterator[String] = sectionMap.keys

    /**
     * Add a section to the configuration.
     *
     * @param sectionName  the new section's name
     *
     * @throws DuplicateSectionException if the section already exists
     */
    protected def addSection(sectionName: String): Unit =
    {
        if (sectionMap contains sectionName)
            throw new DuplicateSectionException(sectionName)

        sectionMap(sectionName) = MutableMap.empty[String, String]
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
    protected def addOption(sectionName: String,
                            optionName: String,
                            value: String)
    {
        if (! hasSection(sectionName))
            throw new NoSuchSectionException(sectionName)

        val optionMap = sectionMap(sectionName)
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
        sectionMap contains sectionName

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
        if (! hasSection(sectionName))
            throw new NoSuchSectionException(sectionName)

        val options = sectionMap(sectionName)
        val canonicalOptionName = transformOptionName(optionName)
        if (! options.contains(canonicalOptionName))
            throw new NoSuchOptionException(canonicalOptionName)

        options(canonicalOptionName)
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

        Map.empty[String, String] ++ sectionMap(sectionName)
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

        sectionMap(sectionName).keys
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
 * Companion object for the <tt>Configuration</tt> class
 */
object Configuration
{
    import java.io.File
    import scala.io.Source

    private val SectionName      = """([^\s\[\]]+)""".r
    private val ValidSection     = ("""^\s*\[""" + 
                                    SectionName.toString +
                                    """\]\s*$""").r
    private val BadSectionFormat = """^\s*(\[[^\]]*)$""".r
    private val BadSectionName   = """^\s*\[(.*)\]\s*$""".r
    private val CommentLine      = """^\s*(#.*)$""".r
    private val BlankLine        = """^(\s*)$""".r
    private val OptionName       = """([-a-zA-Z0-9_]+)""".r
    private val Assignment       = ("""^\s*""" + 
                                    OptionName.toString +
                                    """\s*[:=]\s*(.*)$""").r
    private val FullVariableRef  = (SectionName.toString + """\.""" +
                                    OptionName.toString).r

    /**
     * Read a configuration file.
     *
     * @param path the path to the file.
     *
     * @return the <tt>Configuration</tt> object.
     */
    def apply(path: String): Configuration = apply(Source.fromFile(path))

    /**
     * Read a configuration file.
     *
     * @param file  the file to read
     *
     * @return the <tt>Configuration</tt> object.
     */
    def apply(file: File): Configuration = apply(Source.fromFile(file))

    /**
     * Read a configuration file.
     *
     * @param uri URI to the file
     *
     * @return the <tt>Configuration</tt> object.
     */
    def apply(uri: java.net.URI): Configuration = apply(Source.fromFile(uri))

    /**
     * Read a configuration file.
     *
     * @param source <tt>scala.io.Source</tt> object to read
     *
     * @return the <tt>Configuration</tt> object.
     */
    def apply(source: Source): Configuration = readConfig(source)

    private def readConfig(source: Source): Configuration =
    {
        val config             =  new Configuration
        var curSection: String = null

        object VarResolver
        {
            def get(varName: String): Option[String] =
            {
                try
                {
                    varName match
                    {
                        case FullVariableRef(section, option) => 
                            Some(config.option(section, option))
                        case OptionName(option) => 
                            Some(config.option(curSection, option))
                        case _ =>
                            throw new ConfigException("Reference to " +
                                                      "nonexistent section " +
                                                      "or option: ${" +
                                                      varName + "}")
                    }
                }

                catch
                {
                    case _: NoSuchOptionException =>
                        throw new ConfigException(
                            "In section [" + curSection + "]: Reference to " +
                            "nonexistent option in ${" + varName + "}"
                        )

                    case _: NoSuchSectionException => ""
                        throw new ConfigException(
                            "In section [" + curSection + "]: Reference to " +
                            "nonexistent section in ${" + varName + "}"
                        )
                }
            }
        }

        val template = new UnixShellStringTemplate(VarResolver.get, 
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
                    val newValue = template.substitute(value)
                    config.addOption(curSection, optionName, newValue)
    
                case _ =>
                    throw new ConfigException("Unknown configuration line: \"" +
                                              line + "\"")
            }
        }

        config
    }
}
    

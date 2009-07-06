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
 * The actual configuration parser.
 */
class Configuration(defaultValues: Map[String, String])
{
    // This complicated-looking transform maps the defaultValues map into a new
    // one with the keys all normalized by transformOption().
    val defaults = Map(
        (defaultValues.map((a) => (transformOption(a._1), a._2))).toSeq: _*
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
        val canonicalOptionName = transformOption(optionName)
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
     * Get the list of option names.
     *
     * @param sectionName the section's name
     *
     * @return a list of option names in that section
     *
     * @throws NoSuchSectionException if the section doesn't exist
     */
    def options(sectionName: String): Iterator[String] =
    {
        if (! hasSection(sectionName))
            throw new NoSuchSectionException(sectionName)

        sectionMap(sectionName).keys
    }

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
    def getOption(sectionName: String, optionName: String): String =
    {
        if (! hasSection(sectionName))
            throw new NoSuchSectionException(sectionName)

        val options = sectionMap(sectionName)
        val canonicalOptionName = transformOption(optionName)
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
    def getOptions(sectionName: String): Map[String, String] =
    {
        if (! hasSection(sectionName))
            throw new NoSuchSectionException(sectionName)

        Map.empty[String, String] ++ sectionMap(sectionName)
    }

    /**
     * Transform an option (key) to a consistent form. The default
     * version of this method forces the option name to lower case.
     *
     * @param option  the option name
     *
     * @return the transformed option name
     */
    def transformOption(option: String) = option.toLowerCase
}

object Configuration
{
    import java.io.File
    import scala.io.Source

    private val ValidSection     = """^\s*\[([^\s\[\]]+)\]\s*$""".r
    private val BadSectionFormat = """^\s*(\[[^\]]*)$""".r
    private val BadSectionName   = """^\s*\[(.*)\]\s*$""".r
    private val CommentLine      = """^\s*(#.*)$""".r
    private val Assignment       = """^\s*([-a-zA-Z0-9_.]+)\s*[:=]\s*(.*)$""".r
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
    def apply(source: Source): Configuration =
    {
        val config             =  new Configuration
        var curSection: String = null

        for (line <- Includer(source))
        {
            line match
            {
                case CommentLine(_) =>

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

                    config.addOption(curSection, optionName, value)

                case _ =>
                    throw new ConfigException("Unknown configuration line: \"" +
                                              line + "\"")
            }
        }

        config
    }
}

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

package grizzled.string.template

import scala.util.matching.Regex

/**
 * Base class for all <tt>StringTemplate</tt> exceptions.
 */
class StringTemplateException(val message: String) extends Exception(message)

/**
 * Thrown for non-safe templates when a variable is not found.
 */
class VariableNotFoundException(val variableName: String)
    extends Exception("Variable \"" + variableName + "\" not found.")

/**
 * A simple, configurable string template that substitutes variable references
 * within a string.
 *
 * @param varRegex    The regular expression for a variable reference. Must
 *                    contain a single group that extracts the name.
 * @param resolveVar  A function that takes a variable name as a parameter and
 *                    returns an <tt>Option[String]</tt> value for the variable,
 *                    or <tt>None</tt> if there is no value 
 *                    (<tt>Map[String, String].get()</tt>, for instance).
 * @param safe        <tt>true</tt> for a "safe" template that just substitutes
 *                    a blank string for an unknown variable, <tt>false</tt>
 *                    for one that throws an exception.
 */
class StringTemplate(private val varRegex: Regex, 
                     private val resolveVar: (String) => Option[String],
                     val safe: Boolean)
{
    /**
     * Replace all variable references in the given string. Variable references
     * are recognized per the regular expression passed to the constructor. If
     * a referenced variable is not found in the resolver, this method either:
     *
     * <ul>
     *   <li> throws a <tt>VariableNotFoundException</tt> (if <tt>safe</tt> is
     *        <tt>false</tt>), or
     *   <li> substitutes an empty string (if <tt>safe</tt> is <tt>true</tt>)
     * </ul>
     *
     * Recursive references are supported (but beware of infinite recursion).
     *
     * @param s  the string in which to replace variable references
     *
     * @return the result
     *
     * @throws VariableNotFoundException  a referenced variable could not be
     *                                    found, and <tt>safe</tt> is
     *                                    <tt>false</tt>
     */
    def substitute(s: String): String =
    {
        def doSub(s2: String): String =
        {
            varRegex.findFirstMatchIn(s2) match
            {
                case None =>
                    s2

                case Some(matcher) =>
                    // Don't use Regex.replaceFirstIn, because it wants to
                    // do "$" group substitutions in the replacement string,
                    // which can cause problems if the replacement string
                    // contains embedded "$" characters. Do it manually.
                    val varName = matcher.group(1)
                    var endString = if (matcher.end == s2.length) ""
                                    else s2.substring(matcher.end)
                    val transformed = s2.substring(0, matcher.start) +
                                      getVar(varName) +
                                      endString
                    doSub(transformed)
            }
        }

        doSub(s)
    }

    /**
     * Get a variable's value, returning an empty string or throwing an
     * exception, depending on the setting of <tt>safe</tt>.
     *
     * @param name  the variable name
     *
     * @return the value of the variable
     *
     * @throws VariableNotFoundException  the variable could not be found
     *                                    and <tt>safe</tt> is <tt>false</tt>
     */
    private def getVar(name: String): String =
    {
        resolveVar(name) match
        {
            case None =>
                if (safe)
                    ""
                else
                    throw new VariableNotFoundException(name)

            case Some(value) =>
                value.toString
        }
    }
}

/**
 * A string template that uses the Unix shell-like syntax <tt>${varname}</tt>
 * (or <tt>$varname</tt>) for variable references. A variable's name may consist
 * of alphanumerics and underscores. To include a literal "$" in a string,
 * use two in a row ("$$").
 *
 * @param resolveVar   A function that takes a variable name as a parameter
 *                     and returns an <tt>Option[String]</tt> value for the
 *                     variable, or <tt>None</tt> if there is no value 
 *                     (<tt>Map[String, String].get()</tt>, for instance).
 * @param namePattern  Regular expression pattern to match a variable name, as
 *                     a string (not a Regex). For example: "[a-zA-Z0-9_]+"
 * @param safe         <tt>true</tt> for a "safe" template that just substitutes
 *                     a blank string for an unknown variable, <tt>false</tt>
 *                     for one that throws an exception.
 */
class UnixShellStringTemplate(resolveVar:  (String) => Option[String],
                              namePattern: String,
                              safe:        Boolean)
    extends StringTemplate(("""\$\{?(""" + namePattern + """)\}?""").r, 
                           resolveVar, 
                           safe)
{
    private val EscapedDollar = """(\\*)(\\\$)""".r
    private val RealEscapeToken = "\u0001"
    private val NonEscapeToken  = "\u0002"

    /**
     * Alternate constructor that uses a variable name pattern that permits
     * variable names with alphanumerics and underscore.
     *
     * @param resolveVar   A function that takes a variable name as a parameter
     *                     and returns an <tt>Option[String]</tt> value for the
     *                     variable, or <tt>None</tt> if there is no value 
     *                     (<tt>Map[String, String].get()</tt>, for instance).
     * @param safe         <tt>true</tt> for a "safe" template that just
     *                     substitutes a blank string for an unknown variable,
     *                     <tt>false</tt> for one that throws an exception.
     */
    def this(resolveVar:  (String) => Option[String], safe: Boolean) =
        this(resolveVar, "[a-zA-Z0-9_]+", safe)

    /**
     * Replace all variable references in the given string. Variable references
     * are recognized per the regular expression passed to the constructor. If
     * a referenced variable is not found in the resolver, this method either:
     *
     * <ul>
     *   <li> throws a <tt>VariableNotFoundException</tt> (if <tt>safe</tt> is
     *        <tt>false</tt>), or
     *   <li> substitutes an empty string (if <tt>safe</tt> is <tt>true</tt>)
     * </ul>
     *
     * Recursive references are supported (but beware of infinite recursion).
     *
     * @param s  the string in which to replace variable references
     *
     * @return the result
     *
     * @throws VariableNotFoundException  a referenced variable could not be
     *                                    found, and <tt>safe</tt> is
     *                                    <tt>false</tt>
     */
    override def substitute(s: String): String =
    {
        // Kludge to handle escaped "$". Temporarily replace it with something
        // highly unlikely to be in the string. Then, put a single "$" in its
        // place, after the substitution. Must be sure to handle even versus
        // odd number of backslash characters.

        def preSub(s: String): List[String] =
        {
            val opt = EscapedDollar.findFirstMatchIn(s)
            opt match
            {
                case None =>
                    List(s)

                case Some(m) if ((m.group(1).length % 2) == 0) =>
                    // Odd number of backslashes before "$", including
                    // the one with the dollar token (group 2). Valid escape.
                    List(s.substring(0, m.start(2)), RealEscapeToken) :::
                    preSub(s.substring(m.end(2)))

                case Some(m) =>
                    // Even number of backslashes before "$", including
                    // the one with the dollar token (group 2). Not an escape.
                    List(s.substring(0, m.start(2)), NonEscapeToken) :::
                    preSub(s.substring(m.end(2)))
            }
        }

        val s2 = super.substitute(preSub(s) mkString "")
        s2.replaceAll(RealEscapeToken, """\$""")
          .replaceAll(NonEscapeToken, """\\\$""")
    }
}

/**
 * A string template that uses the cmd Windows.exe syntax
 * <tt>%varname%</tt> for variable references. A variable's name may
 * consist of alphanumerics and underscores. To include a literal "%" in a
 * string, use two in a row ("%%").
 *
 * @param resolveVar   A function that takes a variable name as a parameter
 *                     and returns an <tt>Option[String]</tt> value for the
 *                     variable, or <tt>None</tt> if there is no value 
 *                     (<tt>Map[String, String].get()</tt>, for instance).
 * @param namePattern  Regular expression pattern to match a variable name, as
 *                     a string (not a Regex). For example: "[a-zA-Z0-9_]+"
 * @param safe         <tt>true</tt> for a "safe" template that just substitutes
 *                     a blank string for an unknown variable, <tt>false</tt>
 *                     for one that throws an exception.
 */
class WindowsCmdStringTemplate(resolveVar: (String) => Option[String],
                               namePattern: String,
                               safe:        Boolean)
    extends StringTemplate(("""%(""" + namePattern + """)%""").r, 
                           resolveVar, 
                           safe)
{
    private val EscapedPercent = """%%"""   // regexp string, for replaceAll
    private val Placeholder    = "\u0001"   // temporarily replaces $$

    /**
     * Alternate constructor that uses a variable name pattern that permits
     * variable names with alphanumerics and underscore.
     *
     * @param resolveVar   A function that takes a variable name as a parameter
     *                     and returns an <tt>Option[String]</tt> value for the
     *                     variable, or <tt>None</tt> if there is no value 
     *                     (<tt>Map[String, String].get()</tt>, for instance).
     * @param safe         <tt>true</tt> for a "safe" template that just
     *                     substitutes a blank string for an unknown variable,
     *                     <tt>false</tt> for one that throws an exception.
     */
    def this(resolveVar:  (String) => Option[String], safe: Boolean) =
        this(resolveVar, "[a-zA-Z0-9_]+", safe)

    /**
     * Replace all variable references in the given string. Variable references
     * are recognized per the regular expression passed to the constructor. If
     * a referenced variable is not found in the resolver, this method either:
     *
     * <ul>
     *   <li> throws a <tt>VariableNotFoundException</tt> (if <tt>safe</tt> is
     *        <tt>false</tt>), or
     *   <li> substitutes an empty string (if <tt>safe</tt> is <tt>true</tt>)
     * </ul>
     *
     * Recursive references are supported (but beware of infinite recursion).
     *
     * @param s  the string in which to replace variable references
     *
     * @return the result
     *
     * @throws VariableNotFoundException  a referenced variable could not be
     *                                    found, and <tt>safe</tt> is
     *                                    <tt>false</tt>
     */
    override def substitute(s: String): String =
    {
        // Kludge to handle escaped "%%". Temporarily replace it with something
        // highly unlikely to be in the string. Then, put a single "%" in its
        // place, after the substitution.

        super.substitute(s.replaceAll(EscapedPercent, Placeholder)).
        replaceAll(Placeholder, "%");
    }
}

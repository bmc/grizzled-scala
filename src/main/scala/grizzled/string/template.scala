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

package grizzled.string.template

import scala.util.matching.Regex
import scala.annotation.tailrec

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
 * Information about a parsed variable name.
 */
class Variable(val start: Int, 
               val end: Int, 
               val name: String, 
               val default: Option[String])

/**
 * A simple, configurable string template that substitutes variable references
 * within a string.
 *
 * @param resolveVar  A function that takes a variable name as a parameter and
 *                    returns an <tt>Option[String]</tt> value for the variable,
 *                    or <tt>None</tt> if there is no value 
 *                    (<tt>Map[String, String].get()</tt>, for instance).
 * @param safe        <tt>true</tt> for a "safe" template that just substitutes
 *                    a blank string for an unknown variable, <tt>false</tt>
 *                    for one that throws an exception.
 */
abstract class StringTemplate(val resolveVar: (String) => Option[String],
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
        @tailrec def doSub(s2: String): String =
        {
            findVariableReference(s2) match
            {
                case None =>
                    s2

                case Some(variable) =>
                    var endString = if (variable.end == s2.length) ""
                                    else s2.substring(variable.end)
                    val transformed = s2.substring(0, variable.start) +
                                      getVar(variable.name, variable.default) +
                                      endString
                    doSub(transformed)
            }
        }

        doSub(s)
    }

    /**
     * Parse the location of the first variable in string.
     *
     * @param s  the string
     *
     * @return an <tt>Option[Variable]</tt>, specifying the variable's
     *         location; or <tt>None</tt> if not found
     */
    protected def findVariableReference(s: String): Option[Variable]

    /**
     * Get a variable's value, returning an empty string or throwing an
     * exception, depending on the setting of <tt>safe</tt>.
     *
     * @param name    the variable name
     * @param default default value, or None if there isn't one
     *
     * @return the value of the variable
     *
     * @throws VariableNotFoundException  the variable could not be found
     *                                    and <tt>safe</tt> is <tt>false</tt>
     */
    private def getVar(name: String, default: Option[String]): String =
    {
        resolveVar(name) match
        {
            case None =>
                if (default != None)
                    default.get
                else if (safe)
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
 * escape it with a backslash.
 *
 * <p>For this class, the general form of a variable reference is:</p>
 *
 * <blockquote><pre>${varname?default}</pre></blockquote>
 *
 * <p>The <tt>?default</tt> suffix is optional and specifies a default value
 * to be used if the variable has no value.</p>
 *
 * <p>A shorthand form of a variable reference is:</p>
 *
 * <blockquote><pre>$varname</pre></blockquote>
 *
 * <p>The <i>default</i> capability is not available in the shorthand form.</p>
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
    extends StringTemplate(resolveVar, 
                           safe)
{
    // ${foo} or ${foo?default}
    private var LongFormVariable = ("""\$\{(""" + 
                                    namePattern + 
                                    """)(\?[^}]*)?\}""").r

    // $foo
    private var ShortFormVariable = ("""\$(""" + namePattern + ")").r

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

    /**
     * Parse the location of the first variable in string.
     *
     * @param s  the string
     *
     * @return an <tt>Option[Variable]</tt>, specifying the variable's
     *         location; or <tt>None</tt> if not found
     */
    protected def findVariableReference(s: String): Option[Variable] =
    {
        LongFormVariable.findFirstMatchIn(s) match
        {
            case Some(m1) =>
                val name = m1.group(1)
                val default = m1.group(2) match
                {
                    case null      =>
                        None
                    case s: String =>
                        // Pull off the "?". Can't do Some(s drop 1),
                        // because that yields a RichString, not a String.
                        // Casting doesn't work, either. But assigning to a
                        // temporary string does.
                        val realDefault: String = s drop 1
                        Some(realDefault)
                }

                Some(new Variable(m1.start, m1.end, name, default))

            case None =>
                ShortFormVariable.findFirstMatchIn(s) match
                {
                    case Some(m2) =>
                        Some(new Variable(m2.start, m2.end, 
                                          m2.group(1), None))
                    case None =>
                        None
                }
        }
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
    extends StringTemplate(resolveVar, safe)
{
    private val Variable       = ("""%(""" + namePattern + """)%""").r
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

    /**
     * Parse the location of the first variable in string.
     *
     * @param s  the string
     *
     * @return an <tt>Option[Variable]</tt>, specifying the variable's
     *         location; or <tt>None</tt> if not found
     */
    protected def findVariableReference(s: String): Option[Variable] =
    {
        Variable.findFirstMatchIn(s) match
        {
            case Some(m) =>
                val name = m.group(1)
                Some(new Variable(m.start, m.end, name, None))

            case None =>
                None
        }
    }
}

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

package grizzled.string

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
 * @param varRegex  The regular expression for a variable reference. Must
 *                  contain a single group that extracts the name.
 * @param resolver  An object (e.g., a Map) that has a <tt>get()</tt> method
 *                  that returns a string value for a variable name.
 * @param safe      <tt>true</tt> for a "safe" template that just substitutes
 *                  a blank string for an unknown variable, <tt>false</tt>
 *                  for one that throws an exception.
 */
class StringTemplate(private val varRegex: Regex, 
                     private val resolver: StringTemplate.Resolver,
                     val safe: Boolean)
{
    // Kludge: Have to cast the resolver to an Any/Any type, because
    // structural types work via reflection, and reflection has to work
    // with type erasure. If a Map[String,String] is passed as the
    // resolver, Scala will fail to find a "get(String)" method, because,
    // due to erasure, the REAL method is "get(Any)". Hence this kludged
    // cast. (At least it's hidden from the caller.)
    //
    // The second part of the kludge is to try the first resolver (in case
    // the caller passed in a non-generic object), trap the
    // NoSuchMethodException, and try the second one in the "catch" clause.
    // That's done in the getVar() method.

    private type GenericResolverType = {def get(s: Any): Option[Any]}
    private val genericResolver = resolver.asInstanceOf[GenericResolverType]

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
        // Kludge alert. See docs at the top of the class, above the
        // GenericResolverType declaration.

        val value =
            try
            {
                resolver.get(name)
            }
            catch
            {
                case _: NoSuchMethodException => genericResolver.get(name)
            }

        value match
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
 * Companion object.
 */
object StringTemplate
{
    type Resolver = {def get(s: String): Option[String]}
}

/**
 * A string template that uses the Unix shell-like syntax <tt>${varname}</tt>
 * (or <tt>$varname</tt>) for variable references. A variable's name may consist
 * of alphanumerics and underscores.
 *
 * @param resolver  An object (e.g., a Map) that has a <tt>get()</tt> method
 *                  that returns a string value for a variable name.
 * @param safe      <tt>true</tt> for a "safe" template that just substitutes
 *                  a blank string for an unknown variable, <tt>false</tt>
 *                  for one that throws an exception.
 */
class UnixShellStringTemplate (resolver: StringTemplate.Resolver, safe: Boolean)
    extends StringTemplate("""\$\{?([a-zA-Z0-9_]+)\}?""".r, resolver, safe)

/**
 * A string template that uses the cmd Windows.exe syntax <tt>%varname%</tt>
 * for variable references. A variable's name may consist of alphanumerics and
 * underscores.
 *
 * @param resolver  An object (e.g., a Map) that has a <tt>get()</tt> method
 *                  that returns a string value for a variable name.
 * @param safe      <tt>true</tt> for a "safe" template that just substitutes
 *                  a blank string for an unknown variable, <tt>false</tt>
 *                  for one that throws an exception.
 */
class WindowsCmdStringTemplate (resolver: StringTemplate.Resolver, 
                                safe:     Boolean)
    extends StringTemplate("""%([a-zA-Z0-9_]+)%""".r, resolver, safe)

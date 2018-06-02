package grizzled.string.template

import scala.util.matching.Regex.Match

import scala.util.{Failure, Success, Try}

/** An exception used to signal substitution errors.
  */
final case class SubstitutionException(message: String)
  extends Exception(message)

/** Information about a parsed variable name.
  */
class Variable(val start: Int,
               val end: Int,
               val name: String,
               val default: Option[String])

/** A simple, configurable string template that substitutes variable references
  * within a string.
  *
  * @param resolveVar  A function that takes a variable name as a parameter and
  *                    returns an `Option[String]` value for the variable,
  *                    or `None` if there is no value
  *                    (`Map[String, String].get()`, for instance).
  * @param safe        `true` for a "safe" template that just substitutes
  *                    a blank string for an unknown variable, `false`
  *                    for one that throws an exception.
  */
abstract class StringTemplate(val resolveVar: (String) => Option[String],
                              val safe: Boolean) {

  /** Replace all variable references in the given string. Variable references
    * are recognized per the regular expression passed to the constructor. If
    * a referenced variable is not found in the resolver, this method either:
    *
    * - throws a `VariableNotFoundException` (if `safe` is `false`), or
    * - substitutes an empty string (if `safe` is `true`)
    *
    * Recursive references are supported (but beware of infinite recursion).
    *
    * @param s  the string in which to replace variable references
    *
    * @return `Success(substitutedValue)` or `Failure(error)`
    */
  def sub(s: String): Try[String] = {
    def doSub(s2: String): Try[String] = {

      def subVariable(variable: Variable): Try[String] = {
        val endString = if (variable.end == s2.length) ""
                        else s2.substring(variable.end)
        val before = s2.substring(0, variable.start)

        getVar(variable.name, variable.default).flatMap { subbed =>
          doSub(before + subbed + endString)
        }
      }

      findVariableReference(s2).map { v => subVariable(v) }
                               .getOrElse(Success(s2))
    }

    doSub(s)
  }


  /** Parse the location of the first variable in string.
    *
    * @param s  the string
    *
    * @return an `Option[Variable]`, specifying the variable's
    *         location; or `None` if not found
    */
  protected def findVariableReference(s: String): Option[Variable]

  /** Get a variable's value, returning an empty string or throwing an
    * exception, depending on the setting of `safe`.
    *
    * @param name    the variable name
    * @param default default value, or None if there isn't one
    *
    * @return the value of the variable, in a `Right`, or `Left(error)`
    *         if the variable doesn't exist and `safe` is `false`.
    *                                    and `safe` is `false`
    */
  private def getVar(name: String, default: Option[String]): Try[String] = {

    def handleDefault: Try[String] = {
      default.map(Success(_)).getOrElse {
        if (safe) Success("")
        else Failure(SubstitutionException(s"Variable not found: $name"))
      }
    }

    resolveVar(name).map {s => Success(s)}.getOrElse(handleDefault)
  }
}

/** A string template that uses the Unix shell-like syntax `\${varname}`
  * (or `\$varname`) for variable references. A variable's name may consist
  * of alphanumerics and underscores. To include a literal "$" in a string,
  * escape it with a backslash.
  *
  * For this class, the general form of a variable reference is:
  *
  * {{{
  * \${varname?default}
  * }}}}
  *
  * The `?default` suffix is optional and specifies a default value
  * to be used if the variable has no value.
  *
  * A shorthand form of a variable reference is:
  *
  * {{{
  * \$varname
  * }}}
  *
  * The ''default'' capability is not available in the shorthand form.
  *
  * @param resolveVar   A function that takes a variable name as a parameter
  *                     and returns an `Option[String]` value for the
  *                     variable, or `None` if there is no value
  *                     (`Map[String, String].get()`, for instance).
  * @param namePattern  Regular expression pattern to match a variable name, as
  *                     a string (not a Regex). For example: "[a-zA-Z0-9_]+"
  * @param safe         `true` for a "safe" template that just substitutes
  *                     a blank string for an unknown variable, `false`
  *                     for one that throws an exception.
  */
class UnixShellStringTemplate(resolveVar:  (String) => Option[String],
                              namePattern: String,
                              safe:        Boolean)
  extends StringTemplate(resolveVar, safe) {

  // ${foo} or ${foo?default}
  private val LongFormVariable = ("""\$\{(""" +
                                  namePattern +
                                  """)(\?[^}]*)?\}""").r

  // $foo
  private val ShortFormVariable = ("""\$(""" + namePattern + ")").r

  private val EscapedDollar = """(\\*)(\\\$)""".r
  private val RealEscapeToken = "\u0001"
  private val NonEscapeToken  = "\u0002"

  /** Alternate constructor that uses a variable name pattern that permits
    * variable names with alphanumerics and underscore.
    *
    * @param resolveVar   A function that takes a variable name as a parameter
    *                     and returns an `Option[String]` value for the
    *                     variable, or `None` if there is no value
    *                     (`Map[String, String].get()`, for instance).
    * @param safe         `true` for a "safe" template that just
    *                     substitutes a blank string for an unknown variable,
    *                     `false` for one that throws an exception.
    */
  def this(resolveVar:  (String) => Option[String], safe: Boolean) = {
    this(resolveVar, "[a-zA-Z0-9_]+", safe)
  }

  /** Replace all variable references in the given string. Variable references
    * are recognized per the regular expression passed to the constructor. If
    * a referenced variable is not found in the resolver, this method either:
    *
    * - throws a `VariableNotFoundException` (if `safe` is `false`), or
    * - substitutes an empty string (if `safe` is `true`)
    *
    * Recursive references are supported (but beware of infinite recursion).
    *
    * @param s  the string in which to replace variable references
    *
    * @return `Success(substitutedValue)` or `Failure(error)`
    */
  override def sub(s: String): Try[String] = {
    // Kludge to handle escaped "$". Temporarily replace it with something
    // highly unlikely to be in the string. Then, put a single "$" in its
    // place, after the substitution. Must be sure to handle even versus
    // odd number of backslash characters.

    def preSub(s: String): List[String] = {
      def handleMatch(m: Match): List[String] = {
        if ((m.group(1).length % 2) == 0) {
          // Odd number of backslashes before "$", including
          // the one with the dollar token (group 2). Valid escape.
          List(s.substring(0, m.start(2)), RealEscapeToken) :::
            preSub(s.substring(m.end(2)))
        }

        else {
          // Even number of backslashes before "$", including
          // the one with the dollar token (group 2). Not an escape.
          List(s.substring(0, m.start(2)), NonEscapeToken) :::
            preSub(s.substring(m.end(2)))
        }
      }

      // findFirstMatchIn() returns an Option[Match]. Use map() to
      // invoke handleMatch on the result.

      EscapedDollar.findFirstMatchIn(s).
        map(m => handleMatch(m)).
        getOrElse(List(s))
    }

    super.sub(preSub(s) mkString "").map { s2 =>
      s2.replaceAll(RealEscapeToken, """\$""").
        replaceAll(NonEscapeToken, """\\\$""")
    }
  }

  /** Parse the location of the first variable in string.
    *
    * @param s  the string
    *
    * @return an `Option[Variable]`, specifying the variable's
    *         location; or `None` if not found
    */
  protected def findVariableReference(s: String): Option[Variable] = {
    def handleMatch(m: Match): Option[Variable] = {
      val name = m.group(1)

      val default = m.group(2) match {
        case null      =>
          None
        case s: String =>
          // Pull off the "?". Can't do Some(s drop 1),
          // because that yields a StringOps, not a String.
          // Casting doesn't work, either. But assigning to a
          // temporary string does.
          val realDefault: String = s drop 1
          Some(realDefault)
      }

        Some(new Variable(m.start, m.end, name, default))
    }

    def handleNoMatch: Option[Variable] = {
      ShortFormVariable.findFirstMatchIn(s).
                        map(m => new Variable(m.start,
                                              m.end,
                                              m.group(1),
                                              None))
    }

    LongFormVariable.findFirstMatchIn(s).
                     flatMap(m => handleMatch(m)).
                     orElse(handleNoMatch)
  }
}

/** A string template that uses the cmd Windows.exe syntax `%varname%` for
  * variable references. A variable's name may consist of alphanumerics and
  * underscores. To include a literal "%" in a string, use two in a row
  * ("%%").
  *
  * @param resolveVar   A function that takes a variable name as a parameter
  *                     and returns an `Option[String]` value for the
  *                     variable, or `None` if there is no value
  *                     (`Map[String, String].get()`, for instance).
  * @param namePattern  Regular expression pattern to match a variable name, as
  *                     a string (not a Regex). For example: "[a-zA-Z0-9_]+"
  * @param safe         `true` for a "safe" template that just substitutes
  *                     a blank string for an unknown variable, `false`
  *                     for one that throws an exception.
  */
class WindowsCmdStringTemplate(resolveVar: (String) => Option[String],
                               namePattern: String,
                               safe:        Boolean)
  extends StringTemplate(resolveVar, safe) {

  private val Variable       = ("""%(""" + namePattern + """)%""").r
  private val EscapedPercent = """%%"""   // regexp string, for replaceAll
  private val Placeholder    = "\u0001"   // temporarily replaces $$

  /** Alternate constructor that uses a variable name pattern that permits
    * variable names with alphanumerics and underscore.
    *
    * @param resolveVar   A function that takes a variable name as a parameter
    *                     and returns an `Option[String]` value for the
    *                     variable, or `None` if there is no value
    *                     (`Map[String, String].get()`, for instance).
    * @param safe         `true` for a "safe" template that just
    *                     substitutes a blank string for an unknown variable,
    *                     `false` for one that throws an exception.
    */
  def this(resolveVar:  (String) => Option[String], safe: Boolean) = {
    this(resolveVar, "[a-zA-Z0-9_]+", safe)
  }

  /** Replace all variable references in the given string. Variable references
    * are recognized per the regular expression passed to the constructor. If
    * a referenced variable is not found in the resolver, this method either:
    *
    * <ul>
    * -  throws a `VariableNotFoundException` (if `safe` is
    *        `false`), or
    * -  substitutes an empty string (if `safe` is `true`)
    * </ul>
    *
    * Recursive references are supported (but beware of infinite recursion).
    *
    * @param s  the string in which to replace variable references
    *
    * @return `Right(substitutedValue)` or `Left(error)`
    */
  override def sub(s: String): Try[String] = {
    // Kludge to handle escaped "%%". Temporarily replace it with something
    // highly unlikely to be in the string. Then, put a single "%" in its
    // place, after the substitution.

    super.sub(s.replaceAll(EscapedPercent, Placeholder)).map { s2 =>
      s2.replaceAll(Placeholder, "%")
    }
  }

  /** Parse the location of the first variable in string.
    *
    * @param s  the string
    *
    * @return an `Option[Variable]`, specifying the variable's
    *         location; or `None` if not found
    */
  protected def findVariableReference(s: String): Option[Variable] = {
    def handleMatch(m: Match): Option[Variable] = {
      val name = m.group(1)
      Some(new Variable(m.start, m.end, name, None))
    }

    Variable.findFirstMatchIn(s).flatMap(m => handleMatch(m))
  }
}

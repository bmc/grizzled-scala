package grizzled

import scala.util.matching.Regex

import java.io.{File, IOException}

/**
 * Useful string-related utility functions.
 */
object string
{
    /**
     * An implicit conversion that handles creating a Boolean from a string
     * value. This implicit definition, when in scope, allows code like
     * the following:
     *
     * <blockquote><pre>
     * val flag: Boolean = "true" // implicitly converts "true" to <tt>true</tt>
     * </pre></blockquote>
     *
     * This method currently understands the following strings (in any mixture
     * of upper and lower case). It is currently English-specific.
     *
     * <blockquote>true, t, yes, y, 1<br>false, f, no, n, 0</blockquote>
     *
     * @param s  the string to convert
     *
     * @return a boolean value
     *
     * @throws IllegalArgumentException if <tt>s</tt> cannot be parsed
     */
    implicit def bool(s: String): Boolean =
        s.trim.toLowerCase match
        {
            case "true"  => true
            case "t"     => true
            case "yes"   => true
            case "y"     => true
            case "1"     => true

            case "false" => false
            case "f"     => false
            case "no"    => false
            case "n"     => false
            case "0"     => false

            case _       => 
                throw new IllegalArgumentException("Can't convert string \"" +
                                                   s + "\" to boolean.")
        }

    private lazy val QUOTED_REGEX = """(["'])(?:\\?+.)*?\1""".r
    private lazy val WHITE_SPACE_REGEX = """\s+""".r
    private lazy val QUOTE_SET = Set('\'', '"')

    /**
     * <p>Tokenize a string the way a command line shell would, honoring quoted
     * strings and embedded escaped quotes. Single quoted strings must start
     * and end with single quotes. Double quoted strings must start and end
     * with double quotes. Within quoted strings, the quotes themselves may
     * be backslash-escaped. Quoted and non-quoted tokens may be mixed in
     * the string; quotes are stripped.</p>
     *
     * <p>Examples:</p>
     *
     * <blockquote><pre>
     * val s = """one two "three four" ""
     * for (t <- tokenizeWithQuotes(s)) println("|" + t + "|")
     * // Prints:
     * // |one|
     * // |two|
     * // |three four|
     *
     * val s = """one two 'three "four'"""
     * for (t <- tokenizeWithQuotes(s)) println("|" + t + "|")
     * // Prints:
     * // |one|
     * // |two|
     * // |three "four|
     *
     * val s = """one two 'three \'four ' fiv"e"""
     * for (t <- tokenizeWithQuotes(s)) println("|" + t + "|")
     * // Prints:
     * // |one|
     * // |two|
     * // |three 'four |
     * // |fiv"e|
     * </pre></blockquote>
     *
     * @param s  the string to tokenize
     *
     * @return the tokens, as a list of strings
     */
    def tokenizeWithQuotes(s: String): List[String] =
    {
        def fixedQuotedString(qs: String): String =
        {
            val stripped = qs.substring(1, qs.length - 1)
            if (qs(0) == '"')
                stripped.replace("\\\"", "\"")
            else
                stripped.replace("\\'", "'")
        }

        val trimmed = s.trim()

        if (trimmed.length == 0)
            Nil

        else if (QUOTE_SET.contains(trimmed(0)))
        {
            val mOpt = QUOTED_REGEX.findFirstMatchIn(trimmed)
            if (mOpt == None)  // to eol
                List(trimmed)

            else
            {
                val matched = mOpt.get
                val matchedString = matched.toString
                val token = fixedQuotedString(matchedString)
                val past = trimmed.substring(matched.end)
                List(token) ++ tokenizeWithQuotes(past)
            }
        }

        else
        {
            val mOpt = WHITE_SPACE_REGEX.findFirstMatchIn(trimmed)
            if (mOpt == None) // to eol
                List(trimmed)

            else
            {
                val matched = mOpt.get
                val token = trimmed.substring(0, matched.start)
                val past = trimmed.substring(matched.end)
                List(token) ++ tokenizeWithQuotes(past)
            }
        }
    }
}

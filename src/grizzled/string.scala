package grizzled

import scala.util.matching.Regex

import java.io.{File, IOException}

/**
 * Converts from a string to a boolean
 */
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
}

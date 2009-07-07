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
 * An analog to Scala's <tt>RichString</tt> class, providing some methods
 * that neither <tt>RichString</tt> nor <tt>String</tt> provide.
 */
final class GrizzledString(val string: String)
{
    private lazy val LTrimRegex = """^\s*(.*)$""".r

    /**
     * Trim white space from the front (left) of a string.
     *
     * @param s  string to trim
     *
     * @return possibly modified string
     */
    def ltrim: String =
    {
        LTrimRegex.findFirstMatchIn(string) match
        {
            case Some(m) => m.group(1)
            case None    => string
        }
    }

    private lazy val RTrimRegex = """\s*$""".r

    /**
     * Trim white space from the back (right) of a string.
     *
     * @param s  string to trim
     *
     * @return possibly modified string
     */
    def rtrim: String = RTrimRegex.replaceFirstIn(string, "")

    /**
     * Like perl's <tt>chomp()</tt>: Remove any newline at the end of the
     * line.
     *
     * @param line  the line
     *
     * @return the possibly modified line
     */
    def chomp: String =
        if (string.endsWith("\n"))
            string.substring(0, string.length - 1)
        else
            string

    /**
     * Translate any metacharacters (e.g,. \t, \n, \u2122) into their real
     * characters, and return the translated string. Metacharacter sequences
     * that cannot be parsed (because they're unrecognized, because the Unicode
     * number isn't four digits, etc.) are passed along unchanged.
     *
     * @return the possibly translated string
     */
    def translateMetachars: String =
    {
        import grizzled.parsing.{IteratorStream, Pushback}

        val stream = new IteratorStream[Char](string) with Pushback[Char]

        def parseHexDigits: List[Char] =
        {
            def isHexDigit(c: Char): Boolean =
            {
                try
                {
                    Integer.parseInt(c.toString, 16)
                    true
                }
                catch
                {
                    case _: NumberFormatException => false
                }
            }

            stream.next match
            {
                case Some(c) if isHexDigit(c) =>
                    c :: parseHexDigits
                case Some(c) =>
                    stream.pushback(c)
                    Nil
                case None =>
                    Nil
            }
        }
    
        def parseUnicode: List[Char] =
        {
            val digits = parseHexDigits
            if (digits == Nil)
                Nil
    
            else if (digits.length != 4)
            {
                // Invalid Unicode string.

                List('\\', 'u') ++ digits
            }
    
            else
                List(Integer.parseInt(digits mkString "", 16).
                     asInstanceOf[Char])
        }
    
        def parseMeta: List[Char] =
        {
            stream.next match
            {
                case Some('t')  => List('\t')
                case Some('f')  => List('\f')
                case Some('n')  => List('\n')
                case Some('r')  => List('\r')
                case Some('\\') => List('\\')
                case Some('u')  => parseUnicode
                case Some(c)    => List('\\', c)
                case None       => Nil
            }
        }
    
        def translate: List[Char] =
        {
            stream.next match
            {
                case Some('\\') => parseMeta ::: translate
                case Some(c)    => c :: translate
                case None       => Nil
            }
        }
    
        translate mkString ""
    }
}
    

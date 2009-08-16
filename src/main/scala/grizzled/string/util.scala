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

package grizzled.string

/**
 * Useful string-related utility functions.
 */
object util
{
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

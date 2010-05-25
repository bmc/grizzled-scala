/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2010, Brian M. Clapper
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

package grizzled.parsing

import grizzled.parsing.markup._

import scala.io.Source

/**
 * The <tt>MarkdownParser</tt> class parses the Markdown markup language,
 * producing HTML. The current implementation uses
 * <a href="http://www.mozilla.org/rhino/">Mozilla Rhino</a> and the
 * <a href="http://attacklab.net/showdown/">Showdown</a> Javascript Markdown
 * parser. Consequently, it will not work if you do not have Rhino installed
 * and in your classpath. This software was tested with Rhino 1.7R2.
 */
@deprecated("Use the grizzled.parsing.markup package instead.")
class MarkdownParser
{
    val parser = MarkupParser.getParser(MarkupType.Markdown)

    /**
     * Parse a Markdown document, producing HTML. The generated HTML markup
     * does not contain HTML or BODY tags, so it is suitable for embedded in
     * existing HTML documents.
     *
     * @param markdownSource  The <tt>Source</tt> from which to read the
     *                        lines of Markdown
     * @return the formatted HTML
     */
    @deprecated("Use the grizzled.parsing.markup package instead.")
    def markdownToHTML(markdownSource: Source): String =
        parser.parseToHTML(markdownSource)

    /**
     * Parse a Markdown document, producing HTML. The generated HTML markup
     * does not contain HTML or BODY tags, so it is suitable for embedded in
     * existing HTML documents.
     *
     * @param markdown  The string containing the lines of Markdown
     *
     * @return the formatted HTML
     */
    @deprecated("Use the grizzled.parsing.markup package instead.")
    def markdownToHTML(markdown: String): String =
        parser.parseToHTML(markdown)

    /**
     * Simple wrapper function that produces an XHTML-compliant document,
     * complete with HTML, HEAD and BODY tags, from a Markdown document.
     * The first line of the Markdown document is taken to be the title.
     * This function simply wraps <tt>markdownToHTML()</tt>.
     *
     * @param markdownSource  The <tt>Source</tt> from which to read the
     *                        lines of Markdown
     * @param cssSource       Source for any CSS to be included, or null.
     * @param encoding        The encoding to use. Defaults to "UTF-8".
     *
     * @return the formatted HTML document.
     */
    @deprecated("Use the grizzled.parsing.markup package instead.")
    def markdownToHTMLDocument(markdownSource: Source, 
                               cssSource: Source = null,
                               encoding: String  = "UTF-8"): String =
    {
        val markdown = markdownSource.getLines().toList
        val title = if (markdown.length == 0) "" else markdown.head
        val cssOpt = if (cssSource == null) None else Some(cssSource)

        parser.parseToHTMLDocument(Source.fromString(markdown mkString "\n"),
                                   title, cssOpt, encoding)
    }

    /**
     * Simple wrapper function that produces an XHTML-compliant document,
     * complete with HTML, HEAD and BODY tags, from a Markdown document.
     * The first line of the Markdown document is taken to be the title.
     * This function simply wraps <tt>markdownToHTML()</tt>.
     *
     * @param markdown  The string containing the lines of Markdown
     * @param encoding  The encoding to use.
     *
     * @return the formatted HTML document.
     */
    def markdownToHTMLDocument(markdown: String, encoding: String): String =
        markdownToHTMLDocument(Source.fromString(markdown), null, encoding)

    /**
     * Simple wrapper function that produces an XHTML-compliant document,
     * complete with HTML, HEAD and BODY tags, from a Markdown document.
     * The first line of the Markdown document is taken to be the title.
     * This function simply wraps <tt>markdownToHTML()</tt>.
     *
     * @param markdown  The string containing the lines of Markdown
     *
     * @return the formatted HTML document.
     */
    def markdownToHTMLDocument(markdown: String): String =
        markdownToHTMLDocument(markdown, "UTF-8")
}

/**
 * Object that simplifies access to instances of the <tt>MarkdownParser</tt>
 * class.
 */
@deprecated("Use the grizzled.parsing.markup package instead.")
object Markdown
{
    private lazy val parser = MarkupParser.getParser(MarkupType.Markdown)

    /**
     * Parse a Markdown document, producing HTML. The generated HTML markup
     * does not contain HTML or BODY tags, so it is suitable for embedded in
     * existing HTML documents.
     *
     * @param markdownSource  The <tt>Source</tt> from which to read the
     *                        lines of Markdown
     * @return the formatted HTML
     */
    def toHTML(markdownSource: Source): String =
        parser.parseToHTML(markdownSource)

    /**
     * Parse a Markdown document, producing HTML. The generated HTML markup
     * does not contain HTML or BODY tags, so it is suitable for embedded in
     * existing HTML documents.
     *
     * @param markdown  The string containing the lines of Markdown
     *
     * @return the formatted HTML
     */
    def toHTML(markdown: String): String =
        parser.parseToHTML(markdown)
    
    /**
     * Simple wrapper function that produces an XHTML-compliant document,
     * complete with HTML, HEAD and BODY tags, from a Markdown document.
     * The first line of the Markdown document is taken to be the title.
     * This function simply wraps <tt>toHTML()</tt>.
     *
     * @param markdownSource  The <tt>Source</tt> from which to read the
     *                        lines of Markdown
     * @param cssSource       Source for any CSS to be included, or null.
     * @param encoding        The encoding to use. Defaults to "UTF-8".
     *
     * @return the formatted HTML document.
     */
    def toHTMLDocument(markdownSource: Source, 
                       cssSource: Source = null,
                       encoding: String  = "UTF-8"): String =
    {
        val markdown = markdownSource.getLines().toList
        val title = if (markdown.length == 0) "" else markdown.head
        val cssOpt = if (cssSource == null) None else Some(cssSource)

        parser.parseToHTMLDocument(Source.fromString(markdown mkString "\n"),
                                   title, cssOpt, encoding)
    }

    /**
     * Simple wrapper function that produces an XHTML-compliant document,
     * complete with HTML, HEAD and BODY tags, from a Markdown document.
     * The first line of the Markdown document is taken to be the title.
     * This function simply wraps <tt>toHTML()</tt>.
     *
     * @param markdown  The string containing the lines of Markdown
     * @param encoding  The encoding to use.
     *
     * @return the formatted HTML document.
     */
    def toHTMLDocument(markdown: String, encoding: String): String =
        toHTMLDocument(Source.fromString(markdown), null, encoding)

    /**
     * Simple wrapper function that produces an XHTML-compliant document,
     * complete with HTML, HEAD and BODY tags, from a Markdown document.
     * The first line of the Markdown document is taken to be the title.
     * This function simply wraps <tt>toHTML()</tt>.
     *
     * @param markdown  The string containing the lines of Markdown
     *
     * @return the formatted HTML document.
     */
    def toHTMLDocument(markdown: String): String =
        toHTMLDocument(Source.fromString(markdown))
}

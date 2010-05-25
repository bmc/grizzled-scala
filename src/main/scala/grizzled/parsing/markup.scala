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

/**
 * Defines a common API for various simple markup parsers. Currently, this
 * API supports Markdown and Textile, using different parsers behind a common
 * interface.
 */
package grizzled.parsing.markup

import scala.io.Source

import java.io.File

/**
 * The common parser interface.
 */
trait MarkupParser
{
    /**
     * Convert the specified markup to an HTML fragment, without
     * `html` or `body` tags. The resulting HTML fragment can then be
     * included within an existing HTML document.
     *
     * @param source  the source containing the markup
     *
     * @return the HTML
     */
    def parseToHTML(source: Source): String

    /**
     * Convert the specified markup to an HTML fragment, without
     * `html` or `body` tags. The resulting HTML fragment can then be
     * included within an existing HTML document.
     *
     * @param markup  the string containing the markup
     *
     * @return the HTML
     */
    def parseToHTML(markup: String): String =
        parseToHTML(Source.fromString(markup))

    /**
     * Simple wrapper function that produces an XHTML-compliant document,
     * complete with HTML, HEAD and BODY tags, from a markup document.
     *
     * @param markupSource  The <tt>Source</tt> from which to read the
     *                      lines of markup
     * @param title         The document title
     * @param cssSource     Source for any CSS to be included, or None
     * @param encoding      The encoding to use. Defaults to "UTF-8".
     *
     * @return the formatted HTML document.
     */
    def parseToHTMLDocument(markupSource: Source, 
                            title: String,
                            cssSource: Option[Source] = None,
                            encoding: String  = "UTF-8"): String =
    {
        import scala.xml.parsing.XhtmlParser

        val css = cssSource match
        {
            case None      => ""
            case Some(src) => src.getLines() mkString "\n"
        }

        // Inserting raw HTML in the body will cause it to be escaped. So,
        // parse the HTML into a NodeSeq first. Note the the whole thing
        // has to be wrapped in a single element, so it might as well
        // be the <body> element.

        val htmlBody = "<body>" + parseToHTML(markupSource) + "</body>"
        val htmlBodyNode = XhtmlParser(Source.fromString(htmlBody))

        val contentType = "text/html; charset=" + encoding
        val htmlTemplate =
<html>
<head>
<title>{title}</title>
<style type="text/css">
{css}
</style>
<meta http-equiv="Content-Type" content={contentType}/>
</head>
{htmlBodyNode}
</html>

        htmlTemplate.toString
    }
}

/**
 * Supported markup types.
 */
sealed abstract class MarkupType(val name: String, val mimeType: String)

object MarkupType
{
    case object Markdown extends MarkupType("markdown", "text/markdown")
    case object Textile extends MarkupType("textile", "text/textile")
}

/**
 * Factory object to produce parsers for specific markup document types.
 */
object MarkupParser
{
    import javax.activation.MimetypesFileTypeMap

    private val MimeTypeMap = new MimetypesFileTypeMap

    /**
     * MIME type to MarkupType mapping.
     */
    val MimeTypes = Map(MarkupType.Markdown.mimeType -> MarkupType.Markdown,
                        MarkupType.Textile.mimeType -> MarkupType.Textile)

    /**
     * Parser type to instantiated parser map.
     */
    lazy val MarkupTypeMap = Map(MarkupType.Markdown -> new MarkdownParser,
                                 MarkupType.Textile  -> new TextileParser)

    /**
     * Get a parser for the specified type.
     *
     * @param parserType  the parser type
     */
    def getParser(parserType: MarkupType): MarkupParser = parserType match
    {
        case MarkupType.Markdown => new MarkdownParser
        case MarkupType.Textile  => new TextileParser
    }

    /**
     * Get a parser for the specified MIME type.
     *
     * @param mimeType  the MIME type
     *
     * @return the parser.
     *
     * @throws IllegalArgumentException unsupported MIME type
     */
    def getParser(mimeType: String): MarkupParser =
    MimeTypes.get(mimeType) match
    {
        case None => throw new IllegalArgumentException("Unknown MIME type: " +
                                                        mimeType)
        case Some(parserType) => getParser(parserType)
    }

    /**
     * Get a parser for the specified file.
     *
     * @param f  the file
     *
     *
     * @return the parser.
     *
     * @throws IllegalArgumentException unsupported MIME type
     */
    def getParser(f: File): MarkupParser =
        getParser(MimeTypeMap.getContentType(f))
}

/**
 * The `TextileParser` class parses the Textile markup language, producing
 * HTML. The current implementation uses the Textile parser API from the
 * Eclipse Mylyn WikiText library.
 */
class TextileParser extends MarkupParser
{
    import org.eclipse.mylyn.wikitext.core.parser.{MarkupParser => WTParser}
    import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder
    import org.eclipse.mylyn.wikitext.textile.core.TextileLanguage
    import java.io.StringWriter

    /**
     * Parse a Textile document, producing HTML. The generated HTML markup
     * does not contain HTML or BODY tags, so it is suitable for embedding in
     * existing HTML documents.
     *
     * @param source  source from which to read the lines of Textile
     *
     * @return the formatted HTML
     */
    def parseToHTML(source: Source): String =
    {
        val buf = new StringWriter
        val builder = new HtmlDocumentBuilder(buf)

        builder.setEmitAsDocument(false)

        val parser = new WTParser(new TextileLanguage)
        parser.setBuilder(builder)
        parser.parse(source.getLines() mkString "\n")
        buf.toString
    }
}

/**
 * The `MarkdownParser` class parses the Markdown markup language,
 * producing HTML. The current implementation uses the Scala-based Knockoff
 * parser.
 */
class MarkdownParser extends MarkupParser
{

    /**
     * Parse a Markdown document, producing HTML. The generated HTML markup
     * does not contain HTML or BODY tags, so it is suitable for embedding in
     * existing HTML documents.
     *
     * @param source  The `Source` from which to read the lines of Markdown
     *
     * @return the formatted HTML
     */
    def parseToHTML(source: Source): String =
    {
        import com.tristanhunt.knockoff.DefaultDiscounter._
        toXHTML(knockoff(source mkString "")).toString
    }
}

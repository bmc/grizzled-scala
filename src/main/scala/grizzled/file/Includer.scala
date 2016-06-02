/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright © 2009-2016, Brian M. Clapper
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

package grizzled.file

import grizzled.file.{util => FileUtil}
import java.io.{File, FileWriter}

import scala.io.Source
import scala.annotation.tailrec
import scala.util.Try
import scala.util.matching.Regex
import java.net.{MalformedURLException, URI, URISyntaxException, URL}

import scala.sys.SystemProperties

/** Process "include" directives in files, returning an iterator over
  * lines from the flattened files.
  *
  * The `grizzled.file.Includer` class can be used to process "include"
  * directives within a text file, returning a file-like object. It also
  * contains some utility functions that permit using include-enabled files
  * in other contexts.
  *
  * <h3>Syntax</h3>
  *
  * The <i>include</i> syntax is defined by a regular expression; any
  * line that matches the regular expression is treated as an <i>include</i>
  * directive. The default regular expression, `^%include\s"([^"]+)"$`
  * matches include directives like this:
  *
  * {{{
  * %include "/absolute/path/to/file"
  * %include "../relative/path/to/file"
  * %include "local_reference"
  * %include "http://localhost/path/to/my.config"
  * }}}
  *
  * Relative and local file references are relative to the including file
  * or URL. That is, if an `Includer` is processing file
  * "/home/bmc/foo.txt" and encounters an attempt to include file "bar.txt",
  * it will assume "bar.txt" is to be found in "/home/bmc".
  *
  * Similarly, if an `Includer` is processing URL
  * "http://localhost/bmc/foo.txt" and encounters an attempt to include file
  * "bar.txt", it will assume "bar.txt" is to be found at
  * "http://localhost/bmc/bar.txt".
  *
  * Nested includes are permitted; that is, an included file may, itself,
  * include other files. The maximum recursion level is configurable and
  * defaults to 100.
  *
  * The include syntax can be changed by passing a different regular
  * expression to the `Includer` constructor.
  *
  * <h3>Usage</h3>
  *
  * This package provides an `Includer` class, which processes include
  * directives in a file and behaves somewhat like a Scala `Source`. See the
  * class documentation for more details.
  *
  * The package also provides a `preprocess()` convenience function, via a
  * companion object, that can be used to preprocess a file; it returns the
  * path to the resulting preprocessed file.
  *
  * <h3>Examples</h3>
  *
  * Preprocess a file containing include directives, then read the result:
  *
  * {{{
  * import grizzled.file.Includer
  *
  * Includer(path).foreach(println(_))
  * }}}
  *
  * Use an include-enabled file with a Scala `scala.io.Source`
  * object:
  *
  * {{{
  * import grizzled.file.includer.Includer
  * import scala.io.Source
  *
  * val source = Source.fromFile(Includer.preprocess("/path/to/file"))
  * }}}
  *
  * @param source       the source to read
  * @param includeRegex the regular expression that defines an include directive.
  *                     Must contain a group that surrounds the file or URL part.
  * @param maxNesting   the maximum nesting level
  */
class Includer private(val source: IncludeSource,
                       val includeRegex: Regex,
                       val maxNesting: Int)
extends Iterator[String] {
  /** The stack of sources being read.
    */
  private val sourceStack = new scala.collection.mutable.Stack[IncludeSource]

  sourceStack.push(source)

  /** Determine whether there are any more input lines to be read from the
    * includer.
    *
    * @return `true` if at least one more line is available,
    *         `false` otherwise
    */
  def hasNext: Boolean = {
    @tailrec def somethingHasNext(stack: List[IncludeSource]): Boolean = {
      if (stack.isEmpty)
        false
      else if (stack.head.iterator.hasNext)
        true
      else
        somethingHasNext(stack.tail)
    }

    somethingHasNext(sourceStack.toList.reverse)
  }

  /** Get the next input line. You should call `hasNext` before calling
    * this method, to ensure that there are input lines remaining. Calling
    * `next` on an empty `Includer` will result in a runtime exception
    *
    * @return the next input line
    */
  def next: String = {
    @tailrec def nextFromStack: String = {
      if (sourceStack.isEmpty)
        throw new IllegalStateException("No more data")

      if (sourceStack.top.iterator.hasNext)
        sourceStack.top.iterator.next
      else {
        sourceStack.pop
        nextFromStack
      }
    }

    import grizzled.file.{util => futil}

    @tailrec def processNext: String = {
      val line = nextFromStack

      // NOTE: Could use flatMap(), et al, on the return from
      // findFirstMatchIn(), but this seems more readable.

      line match {
        case includeRegex(inc) if isURL(inc) =>
          val url = new URL(inc)
          sourceStack.push(new IncludeSource(Source.fromURL(url), url.toURI))
          processNext

        case includeRegex(inc) =>
          if (sourceStack.length >= maxNesting)
            throw new IllegalStateException("Max nesting level (" +
                                            maxNesting + ") " +
                                            "exceeded.")

          val curURI =  sourceStack.top.uri
          val parentURI = getParent(curURI)
          val parentPath = parentURI.getPath
          val newPath = parentPath match {
            case "/" => s"$parentPath$inc"
            case _   => s"$parentPath/$inc"
          }

          val newURI    = new URI(parentURI.getScheme,
                                  parentURI.getUserInfo,
                                  parentURI.getHost,
                                  parentURI.getPort,
                                  newPath,
                                  parentURI.getQuery,
                                  parentURI.getFragment)

          val source = Option(newURI.getScheme).getOrElse("file") match {
            case "file" => Source.fromFile(newURI.getPath)
            case _      => Source.fromURL(newURI.toURL)
          }

          sourceStack.push(new IncludeSource(source, newURI))
          processNext

        case _ =>
          if (line endsWith Includer.lineSep)
            line.substring(0, line.length - Includer.lineSep.length)
          else
            line
      }
    }

    processNext
  }

  private def isURL(s: String) = {
    Try {
      new URL(s)
      true
    }
    .recover {
      case _: MalformedURLException =>
        false
    }
    .get
  }

  private def getParent(uri: URI): URI = {
    new URI(uri.getScheme,
            uri.getUserInfo,
            uri.getHost,
            uri.getPort,
            FileUtil.dirname(uri.getPath),
            uri.getQuery,
            uri.getFragment)
  }
}

/** Companion object for the `Includer` class. Also contains some
  * utility methods, such as the `preprocess()` method.
  */
object Includer {
  /** The default regular expression for matching include directives.
    */
  val DefaultIncludeRegex = """^%include\s+"([^"]+)"\s*$""".r

  /** The default maximum nesting level for includes.
    */
  val DefaultMaxNesting   = 100

  private val lineSep = (new SystemProperties).getOrElse("line.separator", "\n")

  /** Create an includer from a `java.io.File`, using the default values for
    * the `maxNesting` and `includeRegex` parameters.
    *
    * @param file the `File` from which to read
    *
    * @return `Success(Includer)` or `Failure(Exception)`
    */
  def apply(file: File): Try[Includer] = {
    apply(file, DefaultIncludeRegex, DefaultMaxNesting)
  }

  /** Create an includer from a `java.io.File`, using the default value for
    * the `maxNesting` parameter.
    *
    * @param file         the `File` from which to read
    * @param includeRegex the regular expression that defines an include
    *                     directive. Must contain a group that surrounds the
    *                     file or URL part.
    *
    * @return `Success(Includer)` or `Failure(Exception)`
    */
  def apply(file: File, includeRegex: Regex): Try[Includer] = {
    apply(file, includeRegex, DefaultMaxNesting)
  }

  /** Create an includer from a `java.io.File`, using the default value
    * for the `includeRegex` parameter.
    *
    * @param file        the `File` to read
    * @param maxNesting  the maximum nesting level
    *
    * @return `Success(Includer)` or `Failure(Exception)`
    */
  def apply(file: File, maxNesting: Int): Try[Includer] = {
    apply(file, DefaultIncludeRegex, maxNesting)
  }

  /** Create an includer from a `java.io.File`.
    *
    * @param file         the `File` from which to read
    * @param includeRegex the regular expression that defines an include
    *                     directive. Must contain a group that surrounds the
    *                     file or URL part.
    * @param maxNesting   the maximum nesting level
    *
    * @return `Success(Includer)` or `Failure(Exception)`
    */
  def apply(file:         File,
            includeRegex: Regex,
            maxNesting:   Int): Try[Includer] = {
    Try {
      new Includer(new IncludeSource(Source.fromFile(file), file.toURI),
                   includeRegex,
                   maxNesting)
    }
  }

  /** Create an includer from a `scala.io.Source`.
    *
    * '''WARNING''': When you read from a `Source`, `Includer` has no
    * reliable way to determine the base URI or file, so ''all'' include
    * references must be absolute. Relative includes ''may'' be supported,
    * depending on what the `Source` is (file, URL, etc.), but you shouldn't
    * count on it.
    *
    * @param source       the `Source` to read
    * @param includeRegex the regular expression that defines an include
    *                     directive. Must contain a group that surrounds the
    *                     file or URL part.
    * @param maxNesting   the maximum nesting level
    *
    * @return `Success(Includer)` or `Failure(Exception)`
    */
  def apply(source:       Source,
            includeRegex: Regex,
            maxNesting:   Int): Try[Includer] = {
    Try {
      new Includer(new IncludeSource(source, new URI(".")),
                   includeRegex,
                   maxNesting)
    }
  }

  /** Create an includer from a `scala.io.Source`, using the default value
    * for the `includeRegex` parameter.
    *
    * '''WARNING''': When you read from a `Source`, `Includer` has no
    * reliable way to determine the base URI or file, so ''all'' include
    * references must be absolute. Relative includes ''may'' be supported,
    * depending on what the `Source` is (file, URL, etc.), but you shouldn't
    * count on it.
    *
    * @param source       the `Source` to read
    * @param maxNesting   the maximum nesting level
    *
    * @return `Success(Includer)` or `Failure(Exception)`
    */
  def apply(source: Source, maxNesting: Int): Try[Includer] = {
    apply(source, DefaultIncludeRegex, maxNesting)
  }

  /** Create an includer from a `scala.io.Source`, using the default value
    * for the `maxNesting` parameter.
    *
    * '''WARNING''': When you read from a `Source`, `Includer` has no
    * reliable way to determine the base URI or file, so ''all'' include
    * references must be absolute. Relative includes ''may'' be supported,
    * depending on what the `Source` is (file, URL, etc.), but you shouldn't
    * count on it.
    *
    * @param source       the `Source` to read
    * @param includeRegex the regular expression that defines an include
    *                     directive. Must contain a group that surrounds the
    *                     file or URL part.
    * @return `Success(Includer)` or `Failure(Exception)`
    */
  def apply(source: Source, includeRegex: Regex): Try[Includer] = {
    apply(source, includeRegex, DefaultMaxNesting)
  }

  /** Create an includer from a `scala.io.Source`, using the default values
    * for the `maxNesting` and `includeRegex` parameters.
    *
    * '''WARNING''': When you read from a `Source`, `Includer` has no
    * reliable way to determine the base URI or file, so ''all'' include
    * references must be absolute. Relative includes ''may'' be supported,
    * depending on what the `Source` is (file, URL, etc.), but you shouldn't
    * count on it.
    *
    * @param source the `Source` to read
    * @return `Success(Includer)` or `Failure(Exception)`
    */
  def apply(source: Source): Try[Includer] = {
    apply(source, DefaultIncludeRegex, DefaultMaxNesting)
  }

  /** Create an includer, using the default value for the
    * `maxNesting` parameter.
    *
    * @param pathOrURI    the path or URI string to read
    * @param includeRegex the regular expression that defines an include
    *                     directive. Must contain a group that surrounds the
    *                     file or URL part.
    * @return `Success(Includer)` or `Failure(Exception)`
    */
  def apply(pathOrURI: String, includeRegex: Regex): Try[Includer] = {
    apply(pathOrURI, includeRegex, DefaultMaxNesting)
  }

  /** Create an includer from a path, using the default value for the
    * `includeRegex` parameter.
    *
    * @param path        the path or URI string to read
    * @param maxNesting  the maximum nesting level
    *
    * @return `Success(Includer)` or `Failure(Exception)`
    */
  def apply(path: String, maxNesting: Int): Try[Includer] = {
    apply(path, DefaultIncludeRegex, maxNesting)
  }

  /** Create an includer from a path, using the default values for the
    * `maxNesting` and `includeRegex` parameters.
    *
    * @param pathOrURI    the path or URI string to read
    */
  def apply(pathOrURI: String): Try[Includer] = {
    apply(pathOrURI, DefaultIncludeRegex, DefaultMaxNesting)
  }

  /** Create an includer.
    *
    * @param pathOrURI    the path or URI string to read
    * @param includeRegex the regular expression that defines an include
    *                     directive. Must contain a group that surrounds the
    *                     file or URL part.
    * @param maxNesting   the maximum nesting level
    * @return `Success(Includer)` or `Failure(Exception)`
    */
  def apply(pathOrURI: String,
            includeRegex: Regex,
            maxNesting: Int): Try[Includer] = {
    Try {
      // Try as a URL first.
      new URL(pathOrURI)
    }
    .map { url: URL =>
      val source = Source.fromURL(url)
      new Includer(new IncludeSource(source, url.toURI), includeRegex, maxNesting)
    }
    .recoverWith {
      case u: MalformedURLException =>
        // Assume file
        Includer(new File(pathOrURI), includeRegex, maxNesting)
    }
  }

  /** Process all include directives in the specified file, returning a
    * path to a temporary file that contains the results of the expansion.
    * The temporary file is automatically removed when the program exits,
    * though the caller is free to remove it whenever it is no longer
    * needed.
    *
    * @param pathOrURI   the path or URI string to read
    * @param tempPrefix  temporary file prefix, with the same meaning as the
    *                    temporary file prefix used by
    *                    `java.io.File.createTempFile()`
    * @param tempSuffix  temporary file suffix, with the same meaning as the
    *                    temporary file suffix used by
    *                    `java.io.File.createTempFile()`
    * @return `Success(path)` where `path` is the path to the temporary file,
    *         or `Failure(exception)`
    */
  def preprocess(pathOrURI:  String,
                 tempPrefix: String,
                 tempSuffix: String): Try[String] = {
    import grizzled.util.withResource

    Includer(pathOrURI, DefaultIncludeRegex, DefaultMaxNesting)
      .map { includer =>
        val fileOut = File.createTempFile(tempPrefix, tempSuffix)
        fileOut.deleteOnExit()

        withResource(new FileWriter(fileOut)) { out =>
          includer.foreach(s => out.write(s + lineSep))
        }

        fileOut.getAbsolutePath
      }
  }
}
/**
  * Used to maintain the stack of sources being read and to keep track of
  * the underlying URI.
  */
private[file] class IncludeSource(val source: Source, val uri: URI) {
  val iterator = source.getLines()
}

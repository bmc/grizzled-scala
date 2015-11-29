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

/** EditLine implementation of the traits defined in the base readline package.
  * Assumes the Java Editline implementation at
  * http://software.clapper.org/java/javaeditline/
  */
package grizzled.readline.editline

import grizzled.readline._
import org.clapper.editline._

/** History implementation that wraps the EditLine history API.
   */
private[editline] class EditLineHistory(val editline: EditLine)
extends History with Util {
  max = Integer.MAX_VALUE

  /** Get the contents of the history buffer, in a list.
    *
    * @return the history lines
    */
  def get: List[String] = editline.getHistory.toList

  /** Clear the history buffer
    */
  def clear = editline.clearHistory

  /** Get the last (i.e., most recent) entry from the buffer.
    *
    * @return the most recent entry, as an `Option`, or
    *         `None` if the history buffer is empty
    */
  def last: Option[String] = str2opt(editline.currentHistoryLine)

  /** Get the current number of entries in the history buffer.
    *
    * @return the size of the history buffer
    */
  def size: Int = editline.historyTotal

  /** Get maximum history size.
    *
    * @return the current max history size, or 0 for unlimited.
    */
  def max: Int = editline.getHistorySize

  /** Set maximum history size.
    *
    * @param newSize the new max history size, or 0 for unlimited.
    */
  def max_=(newSize: Int) = editline.setHistorySize(newSize)

  /** Unconditionally appends the specified line to the history.
    *
    * @param line  the line to add
    */
  protected def append(line: String) = editline.addToHistory(line)
}

/** EditLine implementation of the Readline trait.
  */
private[readline] class EditLineImpl(appName: String,
                                     val autoAddHistory: Boolean)
extends Readline with Util {
  val name = "Java EditLine"
  val editline = EditLine.init(appName)
  val history = new EditLineHistory(editline)
  val self = this

  editline.setCompletionHandler(editlineCompleter)
  editline.setCompletionsDisplayer(editlineCompletionsDisplayer)
  editline.setHistoryUnique(true)

  /** Set the maximum number of completions to show, when there are multiple
    * completions that match a token (if supported by the underlying library).
    *
    * @param max  maximum number of completions to show, or 0 for all
    */
  override def maxShownCompletions_=(max: Int): Unit =
    editline.setMaxShownCompletions(max)

  /** Cleans up, resetting the terminal to its proper state. The default
    * implementation does nothing.
    */
  override def cleanup: Unit = editline.cleanup

  /** Get the maximum number of completions to show, when there are multiple
   * completions that match a token (if supported by the underlying library).
   *
   * @return  maximum number of completions to show, or 0 for all
   */
  override def maxShownCompletions: Int = editline.getMaxShownCompletions

  object editlineCompletionsDisplayer
  extends EditLine.PossibleCompletionsDisplayer {
    def showCompletions(tokens: java.lang.Iterable[String]) = {
      import grizzled.collection.GrizzledIterable._
      import scala.collection.convert.Wrappers._

      val maxTemp = editline.getMaxShownCompletions
      val max = if (maxTemp <= 0) java.lang.Integer.MAX_VALUE else maxTemp
      val all = JIterableWrapper(tokens).toList
      val toShow = all take max
      println("\nPossible completions:")
      println(toShow.columnarize(79))
      if (max < all.length)
        println("[... " + (all.length - max) + " more ...]")
    }
  }

  object editlineCompleter 
  extends EditLine.CompletionHandler with CompleterHelper {
    def complete(token: String, line: String, cursor: Int): Array[String] = {
      import grizzled.parsing.StringToken
      import grizzled.string.GrizzledString._
      import scala.collection.mutable.ArrayBuffer

      val completions = ArrayBuffer.empty[String]

      def mapTokens(tokens: List[StringToken]): List[CompletionToken] = {
        def within(token: StringToken) = {
          val end = token.start + token.string.length
          (cursor >= token.start) && (cursor <= end)
        }

        def before(token: StringToken) = cursor < token.start

        def after(token: StringToken) =
          cursor > (token.start + token.string.length)

        tokens match {
          case Nil => 
            List(Cursor)

          case token :: Nil if (before(token)) =>
            List(Cursor, Delim, LineToken(token.string))

          case token :: Nil if (within(token)) =>
            List(LineToken(token.string), Cursor)

          case token :: Nil if (after(token)) =>
            List(LineToken(token.string), Delim, Cursor)

          case token :: rest if (before(token)) =>
            Cursor :: Delim :: LineToken(token.string) :: Delim ::
          mapWithDelims(rest)

          case token :: rest if (within(token)) =>
            LineToken(token.string) :: Cursor :: Delim ::
          mapWithDelims(rest)

          case token :: rest =>
            LineToken(token.string) :: Delim :: mapTokens(rest)
        }
      }

      def findToken(tokens: List[CompletionToken]): String = {
        tokens match {
          case Nil =>
            ""
          case (Delim | Cursor) :: Nil =>
            ""
          case LineToken(s) :: Cursor :: rest =>
            s
          case LineToken(s) :: Delim :: Cursor :: rest =>
            ""
          case LineToken(s) :: rest =>
            findToken(rest)
          case Cursor :: rest =>
            ""
          case Delim :: rest =>
            findToken(rest)
        }
      }

      // toTokens() comes from GrizzledString. self.completer is the
      // caller-supplied completer object.

      val delims = self.completer.tokenDelimiters
      val tokens = mapTokens(line.toTokens(delims))
      assert (tokens contains Cursor)
      val tokenToComplete = findToken(tokens)
      self.completer.complete(tokenToComplete, tokens, line).toArray
    }
  }

  private[readline] def doReadline(prompt: String): Option[String] = {
    try {
      editline.setPrompt(prompt);
      str2opt(editline.getLine)
    }

    catch {
      case e: java.io.EOFException => None
    }
  }
}

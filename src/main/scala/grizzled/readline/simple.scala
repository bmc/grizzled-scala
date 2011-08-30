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

/** Simple, pure Scala implementation of the traits defined in the base
  * readline package.
  */
package grizzled.readline.simple

import grizzled.readline._

/** Simple history implementation.
  */
private[simple] class SimpleHistory extends History {
  import scala.collection.mutable.ArrayBuffer

  private val history = new ArrayBuffer[String]
  private var maxSize = Integer.MAX_VALUE

  /** Get the contents of the history buffer, in a list.
    *
    * @return the history lines
    */
  def get: List[String] = history.toList

  /** Clear the history buffer
    */
  def clear = history.clear

  /** Get the last (i.e., most recent) entry from the buffer.
    *
    * @return the most recent entry, as an `Option`, or
    *         `None` if the history buffer is empty
    */
  def last: Option[String] = {
    history.length match {
      case 0 => None
      case _ => Some(history.last)
    }
  }

  /** Get the current number of entries in the history buffer.
    *
    * @return the size of the history buffer
    */
  def size: Int = history.length

  /** Get maximum history size.
    *
    * @return the current max history size, or 0 for unlimited.
    */
  def max: Int = maxSize

  /** Set maximum history size.
    *
    * @param newSize the new max history size, or 0 for unlimited.
    */
  def max_=(newSize: Int) {
    maxSize = newSize
    ensureMaxSize
  }

  /** Unconditionally appends the specified line to the history.
    *
    * @param line  the line to add
    */
  protected def append(line: String) = {
    history += line
    ensureMaxSize
  }

  private def ensureMaxSize: Unit = {
    if (history.length > maxSize) {
      // Must convert the newHistory variable to a list, because it's
      // a projection into the real buffer, and it'll go away when the
      // buffer is cleared.
      val newHistory = history.drop(history.length - maxSize).toList
      history.clear
      history.insertAll(0, newHistory)
    }
  }
}

/** Simple implementation of the Readline trait.
  */
private[readline] class SimpleImpl(appName: String, val autoAddHistory: Boolean)
extends Readline with Util {
  import java.io.{InputStreamReader, LineNumberReader}

  val name = "Pure Java"
  val history = new SimpleHistory
  val input = new LineNumberReader(new InputStreamReader(System.in))

  private[readline] def doReadline(prompt: String): Option[String] = {
    try {
      print(prompt)
      str2opt(input.readLine)
    }

    catch {
      case e: java.io.EOFException => None
    }
  }
}

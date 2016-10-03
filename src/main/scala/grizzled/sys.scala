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

package grizzled

/** System-related utility functions and definitions.
  */
object sys {

  /** Indicator of current operating system.
    *
    * - VMS - OpenVMS
    * - Windows - Microsoft Windows, other than Windows CE
    * - WindowsCE - Microsoft Windows CE
    * - OS2 - OS2
    * - NetWare - NetWare
    * - Mac - Mac OS, prior to Mac OS X
    * - Posix - Anything Unix-like, including Mac OS X
    */
  sealed abstract class OperatingSystem(val name: String)
  object OperatingSystem { // For backward compatibility
    case object Posix extends OperatingSystem("Posix")
    case object Mac extends OperatingSystem("Mac OS")
    case object Windows extends OperatingSystem("Windows")
    case object WindowsCE extends OperatingSystem("Windows CE")
    case object OS2 extends OperatingSystem("OS/2")
    case object NetWare extends OperatingSystem("NetWare")
    case object VMS extends OperatingSystem("VMS")
  }

  import OperatingSystem._

  /** The current operating system, a value of the `OperatingSystem`
    * enumeration.
    */
  val os = getOS(System.getProperty("os.name"))

  /** Version of the `os` function that takes an operating system name
    * and returns the `OperatingSystem` enumerated value.
    */
  private val WindowsNameMatch = "^(windows)(.*)$".r
  def getOS(name: String) = {
    val lcName = name.toLowerCase
    val firstToken = lcName.split("""\s""")(0)
    firstToken match {
      case "windows" if lcName == "windows ce" => WindowsCE
      case "windows" if lcName != "windows ce" => Windows
      case "mac"                               => Mac
      case "os/2"                              => OS2
      case "netware"                           => NetWare
      case "openvms"                           => VMS
      case _                                   => Posix
    }
  }
}

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

package grizzled

import scala.util.matching.Regex

import java.io.File

object sys
{
    /**
     * Indicator of current operating system.
     *
     * <ul>
     *   <li>VMS - OpenVMS
     *   <li>Windows - Microsoft Windows, other than Windows CE
     *   <li>WindowsCE - Microsoft Windows CE
     *   <li>OS2 - OS2
     *   <li>NetWare - NetWare
     *   <li>Mac - Mac OS, prior to Mac OS X
     *   <li>Posix - Anything Unix-like, including Mac OS X
     * </ul>
     */
    object OperatingSystem extends Enumeration
    {
        val Posix = Value("Posix")
        val Mac = Value("Mac OS")
        val Windows = Value("Windows")
        val WindowsCE = Value("Windows CE")
        val OS2 = Value("OS/2")
        val NetWare = Value("NetWare")
        val VMS = Value("VMS")
    }

    import OperatingSystem._

    /**
     * The current operating system, a value of the <tt>OperatingSystem</tt>
     * enumeration.
     */
    val os = getOS(System.getProperty("os.name"))

    /**
     * Version of the <tt>os</tt> function that takes an operating system name
     * and returns the <tt>OperatingSystem</tt> enumerated value.
     */
    private val WindowsNameMatch = "^(windows)(.*)$".r
    def getOS(name: String) =
    {
        val lcName = name.toLowerCase
        val firstToken = lcName.split("""\s""")(0)
        firstToken match
        {
            case "windows" if (lcName == "windows ce") => WindowsCE
            case "windows" if (lcName != "windows ce") => Windows
            case "mac"                                 => Mac
            case "os/2"                                => OS2
            case "netware"                             => NetWare
            case "openvms"                             => VMS
            case _                                     => Posix
        }
    }

    /**
     * Get the Java system properties as a Scala iterable. The iterable
     * will produce a (name, value) tuple.
     *
     * @return the system properties as an iterable
     */
    def systemProperties: Iterable[(String, String)] =
    {
        // System.properties aren't for-loopable by themselves.

        import scala.collection.mutable.ArrayBuffer

        val temp = new ArrayBuffer[(String, String)]()
        val e = System.getProperties.propertyNames
        while (e.hasMoreElements)
        {
            val name = e.nextElement.toString
            temp += Tuple(name, System.getProperty(name))
        }

        for (t <- temp)
            yield t
    }
}

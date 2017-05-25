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

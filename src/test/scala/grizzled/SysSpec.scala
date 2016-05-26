package grizzled

import grizzled.sys._

/**
  * Tests the grizzled.file functions.
  */
class SysSpec extends BaseSpec {

  "operating system name" should "be correct" in {
    import OperatingSystem._

    val data = Map("mac"        -> Mac,
                   "windows ce" -> WindowsCE,
                   "windows"    -> Windows,
                   "windows xp" -> Windows,
                   "os/2"       -> OS2,
                   "netware"    -> NetWare,
                   "openvms"    -> VMS,
                   "linux"      -> Posix,
                   "foo"        -> Posix)

    for ((osName, osType) <- data;
         name <- List(osName.capitalize, osName.toUpperCase, osName)) {
      getOS(osName) shouldBe osType
    }
  }
}

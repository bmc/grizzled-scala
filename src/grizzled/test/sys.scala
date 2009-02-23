import org.scalatest.FunSuite
import grizzled.sys._

// FIXME: Need to figure out how to test in a platform-independent way.

/**
 * Tests the grizzled.file functions.
 */
class SysTest extends GrizzledFunSuite
{
    test("system properties")
    {
        for ((k, v) <- systemProperties)
        {
            val javaVal = System.getProperty(k)
            expect(javaVal, "property \"" + k + "\" must be \"" + v + "\"") {v}
        }
    }

    test("operating system override")
    {
        import OperatingSystem._

        val data = Map("mac"        -> Mac,
                       "windows ce" -> WindowsCE,
                       "windows"    -> Windows,
                       "os/2"       -> OS2,
                       "netware"    -> NetWare,
                       "openvms"    -> VMS,
                       "linux"      -> Posix,
                       "foo"        -> Posix)

        for ((osName, osType) <- data;
             name <- List(osName.capitalize, osName.toUpperCase, osName))
        {
            withProperties(Map("grizzled.os.name" -> osName))
            {
                expect(osType, "OS name \"" + osName + "\" -> " + osType) {os}
            }
        }
    }
}

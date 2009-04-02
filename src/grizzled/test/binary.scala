import org.scalatest.FunSuite
import grizzled.binary._

/**
 * Tests the grizzled.binary functions.
 */
class BinaryTest extends GrizzledFunSuite
{
    test("bitCount")
    {
        val intData = Map[Int, Int](
            0                -> 0,
            1                -> 1,
            2                -> 1,
            3                -> 2,
            0x44444444       -> 8,
            0xeeeeeeee       -> 24,
            0xffffffff       -> 32,
            0x7fffffff       -> 31
        )

        val longData = Map[Long, Int](
            0l                   -> 0,
            1l                   -> 1,
            2l                   -> 1,
            3l                   -> 2,
            0x444444444l         -> 9,
            0xeeeeeeeeel         -> 27,
            0xffffffffl          -> 32,
            0x7fffffffl          -> 31,
            0xffffffffffffl      -> 48
        )

        for((n, expected) <- intData)
        {
            expect(expected, "\"" + n.toString + "\" -> " + expected.toString) 
            {
                bitCount(n)
            }
        }

        for((n, expected) <- longData)
        {
            expect(expected, "\"" + n.toString + "\" -> " + expected.toString) 
            {
                bitCount(n)
            }
        }
    }
}

import org.scalatest.FunSuite
import grizzled.string._

/**
 * Tests the grizzled.string functions.
 */
class StringTest extends GrizzledFunSuite
{
    test("string to boolean conversions that should succeed")
    {
        val data = Map(
            "true"  -> true,
            "t"     -> true,
            "yes"   -> true,
            "y"     -> true,
            "1"     -> true,

            "false" -> false,
            "f"     -> false,
            "no"    -> false,
            "n"     -> false,
            "0"     -> false
        )

        for((input, expected) <- data;
            val permutations = List(input,
                                    input.capitalize,
                                    input.toUpperCase,
                                    " " + input,
                                    " " + input + " ",
                                    input + " ");
            s <- permutations)
        {
            expect(expected, "\"" + s + "\" -> " + expected.toString) 
            {
                val b: Boolean = s
                b
            }
        }
    }

    test("string to boolean conversions that should fail")
    {
        val data = List("tru", "tr", "z", "truee", "xtrue",
                        "000", "00", "111", "1a", "0z",
                        "fa", "fal", "fals", "falsee")

        for(input <- data;
            val permutations = List(input,
                                    input.capitalize,
                                    input.toUpperCase);
            s <- permutations)
        {
            intercept[IllegalArgumentException]
            {
                val b: Boolean = s
                b
            }
        }
    }
}

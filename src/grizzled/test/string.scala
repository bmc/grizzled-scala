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

    test("tokenizing quoted strings")
    {
        val data = Map(
            "a b c"                        -> List("a", "b", "c"),
            "aa bb cc"                     -> List("aa", "bb", "cc"),
            "\"aa\\\"a\" 'b'"              -> List("aa\"a", "b"),
            "one two 'three\" four'"       -> List("one", "two", "three\" four"),
            "\"a'b    c'\" 'b\\'c  d' a\"" -> List("a'b    c'", "b'c  d", "a\"")
        )

        for((input, expected) <- data)
        {
            expect(expected, "\"" + input + "\" -> " + expected.toString)
            {
                tokenizeWithQuotes(input)
            }
        }

    }
}

import org.scalatest.FunSuite
import grizzled.file._

class FileTest extends FunSuite
{
    test("basename with empty string should return empty string")
    {
        expect("") { basename("") }
    }

    test("basename of simple path should return itself")
    {
        expect("foo") { "foo" }
    }

    test("basename of a relative path")
    {
        expect("foo")   { basename("foo") }
        expect("bar")  { basename("foo/bar") }
        expect("foo")  { basename("../foo") }
    }

    test("dirname with empty string should return empty string")
    {
        expect("") { dirname("") }
    }

    test("dirname of simple path should return '.'")
    {
        expect(".") { dirname("foo") }
    }

    test("dirname of a relative path")
    {
        expect(".")    { dirname("foo") }
        expect("foo")  { dirname("foo/bar") }
        expect("..")   { dirname("../foo") }
    }

    test("pathsplit")
    {
        expect(("", ""))       { pathsplit("") }
        expect(("", ""))       { pathsplit(null) }
        expect((".", "foo"))   { pathsplit("foo") }
        expect(("foo", "bar")) { pathsplit("foo/bar") }
        expect(("..", "foo"))  { pathsplit("../foo") }
        expect(("/", ""))      { pathsplit("/") }
        expect((".", ""))      { pathsplit(".") }
    }

    test("fnmatch")
    {
        val data = Map(
            ("foo", "f*") -> true,
            ("foo", "f*o") -> true,
            ("foo", "f*b") -> false,
            ("foo", "*") -> true,
            ("foo", "*o") -> true
        )

        for(((string, pattern), expected) <- data)
            expect(expected)   { fnmatch(string, pattern) }
    }
}

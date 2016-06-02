package grizzled.file

import java.io.{File, FileWriter, PrintWriter}

import grizzled.file.{util => fileutil}
import fileutil.withTemporaryDirectory
import grizzled.BaseSpec
import grizzled.util.withResource

import scala.util.Success

class IncluderSpec extends BaseSpec {

  "Includer" should "handle a file including another file" in {
    withTemporaryDirectory("incl") { dir =>
      val input = createTextFile(dir, "outer.txt",
        Array("This is a normal line.",
              """%include "inner.txt"""",
              "This is another normal line.")
      )
      createTextFile(dir, "inner.txt",
        Array("Inner file line 1.",
              "Inner file line 2.")
      )
      Includer(input.getPath).map(_.toVector) shouldBe
        Success(Vector("This is a normal line.",
                       "Inner file line 1.",
                       "Inner file line 2.",
                       "This is another normal line."))
    }
  }

  it should "handle a file including another file including another file" in {
    withTemporaryDirectory("incl") { dir =>
      val input = createTextFile(dir, "main.txt",
        Array("main line 1",
              """%include "inner1.txt"""",
              "main line 3"))
      createTextFile(dir, "inner1.txt",
        Array("inner 1.1",
              """%include "inner2.txt"""",
              "inner 1.3")
      )
      createTextFile(dir, "inner2.txt",
        Array("inner 2.1",
              "inner 2.2")
      )
      Includer(input.getPath).map(_.toVector) shouldBe
        Success(Vector("main line 1",
                       "inner 1.1",
                       "inner 2.1",
                       "inner 2.2",
                       "inner 1.3",
                       "main line 3"))
    }
  }

  it should "handle an include from a URL" in {
    withTemporaryDirectory("incl") { dir =>
      val inner = createTextFile(dir, "inner1.txt",
        Array("inner 1",
              "inner 2")
      )

      val input = createTextFile(dir, "main.txt",
        Array("main line 1",
              s"""%include "${inner.toURI.toURL}"""",
              "main line 3")
      )

      Includer(input.getPath).map(_.toVector) shouldBe
        Success(Vector("main line 1",
                       "inner 1",
                       "inner 2",
                       "main line 3"))
    }
  }

  it should "abort with an exception on a direct recursive include" in {
    withTemporaryDirectory("incl") { dir =>
      val input = createTextFile(dir, "main.txt", Array("""%include "main.txt""""))

      intercept[IllegalStateException] {
        Includer(input.getPath).map(_.toVector).get
      }
    }
  }

  it should "abort with an exception on an indirect recursive include" in {
    withTemporaryDirectory("incl") { dir =>
      val input = createTextFile(dir, "main.txt", Array("""%include "inner.txt""""))
      createTextFile(dir, "inner.txt", Array("""%include "main.txt""""))

      intercept[IllegalStateException] {
        Includer(input.getPath).map(_.toVector).get
      }
    }
  }

  it should "support an alternate include syntax" in {
    withTemporaryDirectory("incl") { dir =>
      val input = createTextFile(dir, "main.txt",
        Array("line 1", "#include 'foo.txt'", "#  include 'bar.txt'", "line 2")
      )
      val foo = createTextFile(dir, "foo.txt", Array("foo"))
      val bar = createTextFile(dir, "bar.txt", Array("bar"))

      val i = Includer(input, """^#\s*include\s*'(.*)'\s*$""".r)
      i.map(_.toVector) shouldBe Success(Vector("line 1", "foo", "bar", "line 2"))
    }
  }

  it should "support an alternate nesting level" in {
    withTemporaryDirectory("incl") { dir =>
      val input = createTextFile(dir, "main.txt",
        Array("line 1", """%include "foo.txt"""", "line 2")
      )
      val foo = createTextFile(dir, "foo.txt", Array("""%include "bar.txt""""))
      val bar = createTextFile(dir, "bar.txt", Array("bar"))

      // The default should work.
      Includer(input).map(_.toVector) shouldBe
        Success(Vector("line 1", "bar", "line 2"))

      intercept[IllegalStateException] {
        Includer(input, 1).map(_.toVector).get
      }
    }
  }

  // --------------------------------------------------------------------------
  // Helpers
  // --------------------------------------------------------------------------

}

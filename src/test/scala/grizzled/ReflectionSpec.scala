package grizzled

import grizzled.reflect._
import scala.reflect.{ClassTag, classTag}

/**
  * Tests the grizzled.file functions.
  */
class ReflectionSpec extends BaseSpec {
  private def isOfTypeTest[T: ClassTag](expected: Boolean, v: Any): Unit = {
    isOfType[T](v) shouldBe expected
  }

  "isOfType primitives" should "work" in {
    isOfTypeTest [Int] (true, 10)
    isOfTypeTest [Int] (false, 10L)

    isOfTypeTest [Long] (true, 10L)
    isOfTypeTest [Long] (false, 10)

    isOfTypeTest [Short] (true, 10.asInstanceOf[Short] )
    isOfTypeTest [Short] (false, 10)

    isOfTypeTest [Float] (true, 10.0f)
    isOfTypeTest [Float] (false, 10)
    isOfTypeTest [Float] (false, 10.0)

    isOfTypeTest [Double] (true, 10.0)
    isOfTypeTest [Double] (false, 10.0f)
    isOfTypeTest [Double] (false, 10)

    isOfTypeTest [Byte] (true, 127.asInstanceOf[Byte] )
    isOfTypeTest [Byte] (false, 127)
    isOfTypeTest [Byte] (false, 10L)
    isOfTypeTest [Byte] (false, 'c')

    isOfTypeTest [Char] (true, 'c')
    isOfTypeTest [Char] (false, 65)
    isOfTypeTest [Char] (false, 65.asInstanceOf[Byte])
  }

  "isOfType non-primitives" should "work" in {
    class Foo
    class Bar extends Foo

    isOfTypeTest [List[Char]] (true, List('a', 'b'))
    isOfTypeTest [Seq[Char]]  (true, List('a', 'b'))
    isOfTypeTest [Char]       (false, List('a', 'b'))
    isOfTypeTest [Foo]        (false, new Object)
    isOfTypeTest [Foo]        (true, new Foo)
    isOfTypeTest [Foo]        (true, new Bar)
    isOfTypeTest [Bar]        (false, new Foo)
  }
}

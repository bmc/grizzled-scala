package grizzled.file

import grizzled.BaseSpec
import grizzled.file.filter._

/** Tests the grizzled.file.filter functions.
  */
class BackslashContinuedLineIteratorSpec extends BaseSpec {
  "BackslashContinuedLineIterator" should "properly join lines" in {
    val data = List[(List[String], List[String])](
      (List("Lorem ipsum dolor sit amet, consectetur \\",
            "adipiscing elit.",
            "In congue tincidunt fringilla. \\",
            "Sed interdum nibh vitae \\",
            "libero",
            "fermentum id dictum risus facilisis."),

       List("Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
            "In congue tincidunt fringilla. Sed interdum nibh vitae libero",
            "fermentum id dictum risus facilisis."))
    )

    for((input, expected) <- data) {
      val iterator = input.iterator
      val result = new BackslashContinuedLineIterator(iterator).toList
      result shouldBe expected
    }
  }

  it should "handle a blank line at the end of the file" in {
    val data = Array("One", "Two", "Three \\", "continued", "Four", "   ")
    val expected = Array("One", "Two", "Three continued", "Four", "   ")

    val result = new BackslashContinuedLineIterator(data.iterator).toArray
    result shouldBe expected
  }

  it should "handle an empty line at the end of the file" in {
    val data = Array("One", "Two", "Three \\", "continued", "Four", "")
    val expected = Array("One", "Two", "Three continued", "Four", "")

    val result = new BackslashContinuedLineIterator(data.iterator).toArray
    result shouldBe expected
  }
}

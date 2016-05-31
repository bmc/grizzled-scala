package grizzled.string

import grizzled.BaseSpec

class WordWrapperSpec extends BaseSpec {

  "WordWrapper" should "wrap strings to 79 columns by default" in {
    val w = new WordWrapper()

    w.wrap("This is a long string that should wrap at a 79-column boundary " +
           "when passed through WordWrapper") shouldBe
      """This is a long string that should wrap at a 79-column boundary when passed
        |through WordWrapper""".stripMargin
  }

  it should "wrap strings to a specified column boundary" in {
    val Column = 40
    val w = new WordWrapper(wrapWidth = Column)

    w.wrap(s"This is a long string that will wrap at column $Column, when " +
            "passed to an appropriately configured WordWrapper.") shouldBe
      s"""This is a long string that will wrap at
         |column $Column, when passed to an
         |appropriately configured WordWrapper.""".stripMargin
  }

  it should "handle indentation" in {
    val Column = 50
    val Indent = 4
    val w = new WordWrapper(wrapWidth = Column, indentation = Indent)
    w.wrap(s"This is a string that will wrap at column $Column and will be " +
           s"indented $Indent characters.") shouldBe
      s"""    This is a string that will wrap at column 50
         |    and will be indented 4 characters.""".stripMargin
  }

  it should "allow a different indentation character" in {
    val Column = 50
    val Indent = 4
    val w = new WordWrapper(wrapWidth = Column, indentation = Indent,
                            indentChar = '-')
    w.wrap(s"This is a string that will wrap at column $Column and will be " +
      s"indented $Indent characters.") shouldBe
      s"""----This is a string that will wrap at column 50
         |----and will be indented 4 characters.""".stripMargin
  }

  it should "handle indenting properly past a prefix" in {
    val Column = 60
    val w = new WordWrapper(wrapWidth = Column, prefix = "error: ")
    w.wrap(s"This is a string that will wrap at column $Column and be " +
           "indented past a prefix string. Each line that wraps should " +
           "be indented properly.") shouldBe
      s"""error: This is a string that will wrap at column 60 and be
         |       indented past a prefix string. Each line that wraps
         |       should be indented properly.""".stripMargin
  }

  it should "ignore specified characters when calculating wrapping" in {
    val Column = 35
    val Ignore = Set('@', '_', '/')
    val w = new WordWrapper(wrapWidth = Column, ignore = Ignore)
    val w2 = new WordWrapper(wrapWidth = Column)
    val s = s"This line should be wrapped at column $Column, but so that " +
             "@it@ ignores the escapes and matches /a line/ that doesn't " +
             "_contain_ those escapes."
    val r = s"""[${Ignore.mkString}]""".r
    val expected = r.replaceAllIn(s, "")
    val postProcessed = r.replaceAllIn(w.wrap(s), "")
    postProcessed shouldBe w2.wrap(expected)
  }

  it should "wrap words appropriately on column boundaries" in {
    val s = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
      "In congue tincidunt fringilla. Sed interdum nibh vitae " +
      "libero fermentum id dictum risus facilisis. Pellentesque " +
      "habitant morbi tristique senectus et netus et malesuada " +
      "fames ac turpis egestas. Sed ante nisi, pharetra ut " +
      "eleifend vitae, congue ut quam. Vestibulum ante ipsum " +
      "primis in."

    val data = Map(
      (s, 79, 0, "", ' ') ->
        """Lorem ipsum dolor sit amet, consectetur adipiscing elit. In congue tincidunt
fringilla. Sed interdum nibh vitae libero fermentum id dictum risus facilisis.
Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac
turpis egestas. Sed ante nisi, pharetra ut eleifend vitae, congue ut quam.
Vestibulum ante ipsum primis in.""",

      (s, 40, 0, "", ' ') ->
        """Lorem ipsum dolor sit amet, consectetur
adipiscing elit. In congue tincidunt
fringilla. Sed interdum nibh vitae
libero fermentum id dictum risus
facilisis. Pellentesque habitant morbi
tristique senectus et netus et malesuada
fames ac turpis egestas. Sed ante nisi,
pharetra ut eleifend vitae, congue ut
quam. Vestibulum ante ipsum primis in.""",

      (s, 40, 5, "", ' ') ->
        """     Lorem ipsum dolor sit amet,
     consectetur adipiscing elit. In
     congue tincidunt fringilla. Sed
     interdum nibh vitae libero
     fermentum id dictum risus
     facilisis. Pellentesque habitant
     morbi tristique senectus et netus
     et malesuada fames ac turpis
     egestas. Sed ante nisi, pharetra ut
     eleifend vitae, congue ut quam.
     Vestibulum ante ipsum primis in.""",

      (s, 60, 0, "foobar: ", ' ') ->
        """foobar: Lorem ipsum dolor sit amet, consectetur adipiscing
        elit. In congue tincidunt fringilla. Sed interdum
        nibh vitae libero fermentum id dictum risus
        facilisis. Pellentesque habitant morbi tristique
        senectus et netus et malesuada fames ac turpis
        egestas. Sed ante nisi, pharetra ut eleifend vitae,
        congue ut quam. Vestibulum ante ipsum primis in.""",

      (s, 60, 0, "foobar: ", '.') ->
        """foobar: Lorem ipsum dolor sit amet, consectetur adipiscing
........elit. In congue tincidunt fringilla. Sed interdum
........nibh vitae libero fermentum id dictum risus
........facilisis. Pellentesque habitant morbi tristique
........senectus et netus et malesuada fames ac turpis
........egestas. Sed ante nisi, pharetra ut eleifend vitae,
........congue ut quam. Vestibulum ante ipsum primis in."""

    )

    for((input, expected) <- data) {
      val (string, width, indent, prefix, indentChar) = input
      val wrapper = WordWrapper(wrapWidth   = width,
        indentation = indent,
        prefix      = prefix,
        indentChar  = indentChar)
      wrapper.wrap(string) shouldBe expected
    }
  }

}

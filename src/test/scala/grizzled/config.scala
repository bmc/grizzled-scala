
import org.scalatest.FunSuite
import scala.io.Source
import grizzled.config._

/** Test the Configuration class.
  */
class config extends FunSuite {

  val TestConfig =
    """
      |[section1]
      |o1 = val1
      |o2: val2
      |o3: ${o9}
      |o4: ${o1}
      |o5: ${section2.o1}
      |[section2]
      |o1: foo
    """.stripMargin

  test("successful option retrieval") {
    val cfgRes = Configuration(Source.fromString(TestConfig))
    assertResult(true, "Instantiate Configuration") { cfgRes.isRight }
    val cfg = cfgRes.right.get

    val testData = Map[(String, String), Option[String]](
      ("section1", "o1") -> Some("val1"),
      ("section1", "o2") -> Some("val2"),
      ("section1", "o3") -> Some(""),
      ("section1", "o4") -> Some("val1"),
      ("section1", "o5") -> Some("foo")
    )

    for (((section, option), expected) <- testData) {
      assertResult(expected, s"Retrieval of $section.$option") {
        cfg.get(section, option)
      }
    }
  }

  test("not found function") {
    def notFound(section: String, option: String):
      Either[String, Option[String]] = {

      Right(Some(s"NF:$section.$option"))
    }

    val cfgRes = Configuration(Source.fromString(TestConfig),
                               notFoundFunction = Some(notFound _))
    assertResult(true, "Instantiate Configuration") { cfgRes.isRight }
    val cfg = cfgRes.right.get


    assertResult(Some("val1"), "Retrieval of existing value") {
      cfg.get("section1", "o1")
    }

    assertResult(Some("NF:section1.foo"), "Nonexistent option from existing section") {
      cfg.get("section1", "foo")
    }

    assertResult(Some("NF:section9999.foobar"), "Option from nonexistent section") {
      cfg.get("section9999", "foobar")
    }
  }
}

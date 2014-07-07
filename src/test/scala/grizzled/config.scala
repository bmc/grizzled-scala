
import org.scalatest.FlatSpec
import scala.io.Source
import grizzled.config._

/** Test the Configuration class.
  */
class config extends FlatSpec {

  import Configuration.Implicits._

  object Fixture {
    val TestConfig =
      """
        |[section1]
        |o1 = val1
        |o2: val2
        |o3: ${o9}
        |o4: ${o1}
        |o5: ${section2.o1}
        |intOpt: ${section3.intOpt}
        |longOpt: ${section3.longOpt}
        |[section2]
        |o1: foo
        |substError: $x
        |[section3]
        |intOpt: 10
        |boolOptTrue: true
        |boolOptFalse: false
        |boolOptYes: true
        |longOpt: 9223372036854775807
        |charOpt: c
      """.stripMargin

    val cfg = Configuration(Source.fromString(TestConfig)).right.get

    val TestConfigWithExoticSection =
      """
        |[section1]
        |foo=bar
        |[section1.1]
        |bar=baz
      """.stripMargin
  }

  "A Configuration object" should "return values properly" in {

    val cfg = Fixture.cfg

    val testData = Map[(String, String), Option[String]](
      ("section1", "o1") -> Some("val1"),
      ("section1", "o2") -> Some("val2"),
      ("section1", "o3") -> Some(""),
      ("section1", "o4") -> Some("val1"),
      ("section1", "o5") -> Some("foo")
    )

    for (((section, option), expected) <- testData) {
      assert(cfg.get(section, option) === expected)
    }
  }

  it should "support a 'not found' function" in {
    def notFound(section: String, option: String):
      Either[String, Option[String]] = {

      Right(Some(s"NF:$section.$option"))
    }

    val cfg = Configuration(Source.fromString(Fixture.TestConfig),
                            notFoundFunction = Some(notFound _)).right.get

    assert(Some("val1") === cfg.get("section1", "o1"))
    assert(Some("NF:section1.foo") === cfg.get("section1", "foo"))
    assert(Some("NF:section9999.foobar") === cfg.get("section9999", "foobar"))
  }

  it should "handle getSection() calls" in {
    val cfg = Fixture.cfg
    val section1 = cfg.getSection("section1")

    assert(section1 != None)
    assert(section1.get.options.get("o2") !== None)
    assert(section1.get.options.get("o2") === Some("val2"))
    assert(section1.get.options.get("o99") === None)
  }

  it should "properly expand variables in getSection()" in {
    val cfg = Fixture.cfg

    assert(cfg.getSection("section1").get.options.get("o4") === Some("val1"))
    assert(cfg.getSection("section1").get.options.get("o5") === Some("foo"))
  }

  it should "support asOpt[Int]" in {
    val cfg = Fixture.cfg

    assert(cfg.asOpt[Int]("section3", "intOpt") === Some(10))
    assert(cfg.asOpt[Int]("section3", "noSuchOption") === None)
    assert(cfg.asOpt[Int]("section3", "boolOptTrue") === None)
  }

  it should "support asOpt[Long]" in {
    val cfg = Fixture.cfg

    assert(cfg.asOpt[Long]("section3", "intOpt") === Some(10L))
    assert(cfg.asOpt[Long]("section3", "noSuchOption") === None)
    assert(cfg.asOpt[Long]("section3", "boolOptTrue") === None)
    assert(cfg.asOpt[Long]("section3", "longOpt") === Some(9223372036854775807L))
    assert(cfg.asOpt[Long]("section1", "longOpt") === Some(9223372036854775807L))
  }

  it should "support asOpt[Boolean]" in {
    val cfg = Fixture.cfg

    assert(cfg.asOpt[Boolean]("section3", "intOpt") === None)
    assert(cfg.asOpt[Boolean]("section3", "noSuchOption") === None)
    assert(cfg.asOpt[Boolean]("section3", "boolOptTrue") === Some(true))
    assert(cfg.asOpt[Boolean]("section3", "boolOptFalse") === Some(false))
  }

  it should "support asOpt[String]" in {
    val cfg = Fixture.cfg

    assert(cfg.asOpt[String]("section3", "intOpt") === Some("10"))
    assert(cfg.asOpt[String]("section3", "noSuchOption") === None)
    assert(cfg.asOpt[String]("section3", "boolOptTrue") === Some("true"))
    assert(cfg.asOpt[String]("section3", "boolOptFalse") === Some("false"))
  }

  it should "support asOpt[Character]" in {
    val cfg = Fixture.cfg

    assert(cfg.asOpt[Character]("section3", "charOpt") === Some('c'))
    assert(cfg.asOpt[Character]("section3", "intOpt") === None)
    assert(cfg.asOpt[Character]("section3", "noSuchOption") === None)
    assert(cfg.asOpt[Character]("section3", "boolOptTrue") === None)
    assert(cfg.asOpt[Character]("section3", "boolOptFalse") === None)
  }

  it should "support asEither[Int]" in {
    val cfg = Fixture.cfg

    assert(cfg.asEither[Int]("section3", "intOpt") === Right(Some(10)))
    assert(cfg.asEither[Int]("section1", "intOpt") === Right(Some(10)))
    assert(cfg.asEither[Int]("section3", "noSuchOption") === Right(None))
    assert(cfg.asEither[Int]("section3", "boolOptTrue").isLeft === true)
  }

  it should "support asEither[Long]" in {
    val cfg = Fixture.cfg

    assert(cfg.asEither[Long]("section3", "intOpt") === Right(Some(10)))
    assert(cfg.asEither[Long]("section1", "intOpt") === Right(Some(10)))
    assert(cfg.asEither[Long]("section3", "noSuchOption") === Right(None))
    assert(cfg.asEither[Long]("section3", "boolOptTrue").isLeft === true)
    assert(cfg.asEither[Long]("section3", "longOpt") === Right(Some(9223372036854775807L)))
  }

  it should "support asEither[Boolean]" in {
    val cfg = Fixture.cfg

    assert(cfg.asEither[Boolean]("section3", "intOpt").isLeft === true)
    assert(cfg.asEither[Boolean]("section3", "noSuchOption") === Right(None))
    assert(cfg.asEither[Boolean]("section3", "boolOptTrue") === Right(Some(true)))
    assert(cfg.asEither[Boolean]("section3", "boolOptFalse") === Right(Some(false)))
  }

  it should "support asEither[String]" in {
    val cfg = Fixture.cfg

    assert(cfg.asEither[String]("section3", "intOpt") === Right(Some("10")))
    assert(cfg.asEither[String]("section3", "noSuchOption") === Right(None))
    assert(cfg.asEither[String]("section3", "boolOptTrue") === Right(Some("true")))
    assert(cfg.asEither[String]("section3", "boolOptFalse") === Right(Some("false")))
  }

  it should "support asEither[Character]" in {
    val cfg = Fixture.cfg

    assert(cfg.asEither[Character]("section3", "charOpt") === Right(Some('c')))
    assert(cfg.asEither[Character]("section3", "intOpt").isLeft)
    assert(cfg.asEither[Character]("section3", "noSuchOption") === Right(None))
    assert(cfg.asEither[Character]("section3", "boolOptTrue").isLeft)
    assert(cfg.asEither[Character]("section3", "boolOptFalse").isLeft)
  }

  it should "handle unsafe substitutions" in {
    val cfg = Configuration(Source.fromString(Fixture.TestConfig),
                            safe = false).right.get

    assert(cfg.asEither[String]("section2", "substError").isLeft)
    assert(cfg.asEither[Int]("section3", "intOpt") === Right(Some(10)))
    assert(cfg.asEither[Int]("section1", "intOpt") === Right(Some(10)))
  }

  it should "detect illegal characters in section names" in {
    val cfg = Configuration(Source.fromString(Fixture.TestConfigWithExoticSection))
    assert(cfg.isLeft)
  }

  it should "honor given SectionNamePattern when loading data" in {
    val cfg = Configuration(Source.fromString(Fixture.TestConfigWithExoticSection),
      sectionNamePattern = """([a-zA-Z0-9_\.]+)""".r)
    assert(cfg.isRight)
    val loadedCfg = cfg.right.get
    assert(loadedCfg.get("section1.1", "bar") === Some("baz"))
  }
}

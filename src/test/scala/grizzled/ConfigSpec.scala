package grizzled.config

import grizzled.BaseSpec
import scala.io.Source
import scala.util.{Success, Try}

/** Test the Configuration class.
  */
class ConfigSpec extends BaseSpec {

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
        |o6: Continued \
        |line
        |intOpt: ${section3.intOpt}
        |longOpt: ${section3.longOpt}
        |valueWithNewline: line 1\nline 2
        |endsWithTrademark: Foobar\u2122
        |rawValue -> \tShould not expand\n
        |
        |[section2]
        |o1: foo
        |substError: $x
        |
        |[section3]
        |intOpt: 10
        |boolOptTrue: true
        |boolOptFalse: false
        |boolOptYes: true
        |longOpt: 9223372036854775807
        |charOpt: c
      """.stripMargin + "\n"

    val cfg = Configuration.read(Source.fromString(TestConfig)).get

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
      cfg.get(section, option) shouldBe expected
    }
  }

  it should "allow + to replace an option without mutating the original" in {
    val eCfg = Configuration.read(Source.fromString(Fixture.TestConfig))
    eCfg shouldBe Symbol("success")

    val cfg: Configuration = eCfg.get
    val newCfg = cfg + ("section2", "o1", "bar")

    cfg.asOpt[String]("section2", "o1") shouldBe Some("foo")
    newCfg.asOpt[String]("section2", "o1") shouldBe Some("bar")
  }

  it should "allow + to addFile an option without mutating the original" in {
    val cfg = Fixture.cfg
    val newCfg = cfg + ("section99", "opt1", "something")

    cfg.asOpt[String]("section99", "opt1") shouldBe None
    newCfg.asOpt[String]("section99", "opt1") shouldBe Some("something")
  }

  it should "allow - to remove a non-existing section" in {
    val cfg = Fixture.cfg
    val newCfg = cfg - ("section-xxx", "option")
    cfg shouldBe newCfg
  }

  it should "allow - to remove a section with only one option" in {
    val cfg = Fixture.cfg + ("section999", "option", "value")
    val newCfg = cfg - ("section999", "option")

    cfg.hasSection("section999") shouldBe true
    cfg.get("section999", "option") shouldBe Some("value")
    newCfg.hasSection("section999") shouldBe false
    newCfg.get("section999", "option") shouldBe None
  }

  it should "allow - to remove an option from a section with many options" in {
    val cfg = Fixture.cfg
    val newCfg = cfg - ("section1", "o1")

    cfg.hasSection("section1") shouldBe true
    newCfg.hasSection("section1") shouldBe true
    val cfgOptionNames = cfg.optionNames("section1").toSet
    cfgOptionNames.contains("o1") shouldBe true
    val newCfgOptionNames = newCfg.optionNames("section1").toSet
    newCfgOptionNames.contains("o1") shouldBe false
    newCfgOptionNames.size shouldBe (cfgOptionNames.size - 1)
  }

  it should "allow addition of multiple new sections and options with ++" in {
    val cfg = Fixture.cfg
    val newCfg = cfg ++ ("section999" -> ("opt1" -> "value1"),
                         "section888" -> ("opt2" -> "value2"),
                         "section999" -> ("opt3" -> "value3"))
    cfg.hasSection("section999") shouldBe false
    cfg.hasSection("section888") shouldBe false

    newCfg.get("section999", "opt1") shouldBe Some("value1")
    newCfg.get("section999", "opt2") shouldBe None
    newCfg.get("section999", "opt3") shouldBe Some("value3")
    newCfg.get("section888", "opt2") shouldBe Some("value2")
  }

  it should "allow addition of new options to existing sections with ++" in {
    val cfg = Fixture.cfg
    val newCfg = cfg ++ ("section1" -> ("newOption1" -> "value1"),
                         "section2" -> ("newOption2" -> "value2"),
                         "section1" -> ("newOption3" -> "value3"))

    cfg.hasSection("section1") shouldBe true
    cfg.hasSection("section2") shouldBe true
    newCfg.hasSection("section1") shouldBe true
    newCfg.hasSection("section2") shouldBe true

    val oldSection1Keys = cfg.options("section1").keySet
    val oldSection2Keys = cfg.options("section2").keySet
    val newSection1Keys = newCfg.options("section1").keySet
    val newSection2Keys = newCfg.options("section2").keySet

    (newSection1Keys -- oldSection1Keys) shouldBe Set("newOption1", "newOption3")
    (newSection2Keys -- oldSection2Keys) shouldBe Set("newOption2")
  }

  it should "allow replacing options in existing sections with ++" in {
    val cfg = Fixture.cfg
    val newCfg = cfg ++ ("section1" -> ("o1" -> "newValue1"),
                         "section1" -> ("o2" -> "newValue2"))

    cfg.hasSection("section1") shouldBe true
    newCfg.hasSection("section1") shouldBe true

    val oldSection1Keys = cfg.options("section1").keySet
    val newSection1Keys = newCfg.options("section1").keySet

    newSection1Keys shouldBe oldSection1Keys
    cfg.get("section1", "o1") shouldBe Some("val1")
    newCfg.get("section1", "o1") shouldBe Some("newValue1")
    newCfg.get("section1", "o2") shouldBe Some("newValue2")
  }

  it should "allow addition of a new map of values with ++" in {
    val cfg = Fixture.cfg
    val newCfg = cfg ++ Map(
      "section999"  -> Map("opt1" -> "value1",
                           "opt2" -> "value2"),
      "section1000" -> Map("opt3" -> "value3")
    )

    cfg.hasSection("section999") shouldBe false
    cfg.hasSection("section888") shouldBe false

    newCfg.get("section999", "opt1") shouldBe Some("value1")
    newCfg.get("section999", "opt2") shouldBe Some("value2")
    newCfg.get("section1000", "opt3") shouldBe Some("value3")
  }

  it should "allow removal of existing options via --" in {
    val cfg = Fixture.cfg
    val newCfg = cfg -- Seq("section1" -> "o1", "section1" -> "o2")

    cfg.get("section1", "o1") shouldBe Some("val1")
    cfg.get("section1", "o2") shouldBe Some("val2")
    newCfg.get("section1", "o1") shouldBe None
    newCfg.get("section1", "o2") shouldBe None
  }

  it should "allow removal of nonexistent options via --" in {
    val cfg = Fixture.cfg
    val newCfg = cfg -- Seq("section1" -> "o99999", "section1" -> "x99999")

    cfg shouldBe newCfg
  }

  it should "remove sections that are empty after --" in {
    val cfg = Fixture.cfg
    val optionsToRemove = cfg.options("section1").keySet.map { opt =>
      "section1" -> opt
    }.toSeq

    val newCfg = cfg -- optionsToRemove
    newCfg.hasSection("section1") shouldBe false
  }

  it should "support a 'not found' function" in {
    def notFound(section: String, option: String): Try[Option[String]] = {
      Success(Some(s"NF:$section.$option"))
    }

    val cfg = Configuration.read(Source.fromString(Fixture.TestConfig),
                                 notFoundFunction = Some(notFound _)).get

    Some("val1") shouldBe cfg.get("section1", "o1")
    Some("NF:section1.foo") shouldBe cfg.get("section1", "foo")
    Some("NF:section9999.foobar") shouldBe cfg.get("section9999", "foobar")
  }

  it should "handle getSection() calls" in {
    val cfg = Fixture.cfg
    val section1 = cfg.getSection("section1")

    section1 shouldBe Symbol("defined")
    section1.get.options.get("o2") !== None
    section1.get.options.get("o2") shouldBe Some("val2")
    section1.get.options.get("o99") shouldBe None
  }

  it should "properly expand variables in getSection()" in {
    val cfg = Fixture.cfg

    cfg.getSection("section1").get.options.get("o4") shouldBe Some("val1")
    cfg.getSection("section1").get.options.get("o5") shouldBe Some("foo")
  }

  it should "properly expand metacharacters in values" in {
    val cfg = Fixture.cfg

    cfg.asOpt[String]("section1", "valueWithNewline") shouldBe Some("line 1\nline 2")
    cfg.asOpt[String]("section1", "endsWithTrademark") shouldBe Some("Foobar\u2122")
  }

  it should "not expand metachars in raw values" in {
    val cfg = Fixture.cfg

    cfg.asOpt[String]("section1", "rawValue") shouldBe Some("\\tShould not expand\\n")
  }

  it should "support asOpt[Int]" in {
    val cfg = Fixture.cfg

    cfg.asOpt[Int]("section3", "intOpt") shouldBe Some(10)
    cfg.asOpt[Int]("section3", "noSuchOption") shouldBe None
    cfg.asOpt[Int]("section3", "boolOptTrue") shouldBe None
  }

  it should "support asOpt[Long]" in {
    val cfg = Fixture.cfg

    cfg.asOpt[Long]("section3", "intOpt") shouldBe Some(10L)
    cfg.asOpt[Long]("section3", "noSuchOption") shouldBe None
    cfg.asOpt[Long]("section3", "boolOptTrue") shouldBe None
    cfg.asOpt[Long]("section3", "longOpt") shouldBe Some(9223372036854775807L)
    cfg.asOpt[Long]("section1", "longOpt") shouldBe Some(9223372036854775807L)
  }

  it should "support asOpt[Boolean]" in {
    val cfg = Fixture.cfg

    cfg.asOpt[Boolean]("section3", "intOpt") shouldBe None
    cfg.asOpt[Boolean]("section3", "noSuchOption") shouldBe None
    cfg.asOpt[Boolean]("section3", "boolOptTrue") shouldBe Some(true)
    cfg.asOpt[Boolean]("section3", "boolOptFalse") shouldBe Some(false)
  }

  it should "support asOpt[String]" in {
    val cfg = Fixture.cfg

    cfg.asOpt[String]("section3", "intOpt") shouldBe Some("10")
    cfg.asOpt[String]("section3", "noSuchOption") shouldBe None
    cfg.asOpt[String]("section3", "boolOptTrue") shouldBe Some("true")
    cfg.asOpt[String]("section3", "boolOptFalse") shouldBe Some("false")
  }

  it should "support asOpt[Character]" in {
    val cfg = Fixture.cfg

    cfg.asOpt[Character]("section3", "charOpt") shouldBe Some('c')
    cfg.asOpt[Character]("section3", "intOpt") shouldBe None
    cfg.asOpt[Character]("section3", "noSuchOption") shouldBe None
    cfg.asOpt[Character]("section3", "boolOptTrue") shouldBe None
    cfg.asOpt[Character]("section3", "boolOptFalse") shouldBe None
  }

  it should "support asEither[Int]" in {
    val cfg = Fixture.cfg

    cfg.asEither[Int]("section3", "intOpt") shouldBe Right(Some(10))
    cfg.asEither[Int]("section1", "intOpt") shouldBe Right(Some(10))
    cfg.asEither[Int]("section3", "noSuchOption") shouldBe Right(None)
    cfg.asEither[Int]("section3", "boolOptTrue").isLeft shouldBe true
  }

  it should "support asEither[Long]" in {
    val cfg = Fixture.cfg

    cfg.asEither[Long]("section3", "intOpt") shouldBe Right(Some(10))
    cfg.asEither[Long]("section1", "intOpt") shouldBe Right(Some(10))
    cfg.asEither[Long]("section3", "noSuchOption") shouldBe Right(None)
    cfg.asEither[Long]("section3", "boolOptTrue").isLeft shouldBe true
    cfg.asEither[Long]("section3", "longOpt") shouldBe Right(Some(9223372036854775807L))
  }

  it should "support asEither[Boolean]" in {
    val cfg = Fixture.cfg

    cfg.asEither[Boolean]("section3", "intOpt").isLeft shouldBe true
    cfg.asEither[Boolean]("section3", "noSuchOption") shouldBe Right(None)
    cfg.asEither[Boolean]("section3", "boolOptTrue") shouldBe Right(Some(true))
    cfg.asEither[Boolean]("section3", "boolOptFalse") shouldBe Right(Some(false))
  }

  it should "support asEither[String]" in {
    val cfg = Fixture.cfg

    cfg.asEither[String]("section3", "intOpt") shouldBe Right(Some("10"))
    cfg.asEither[String]("section3", "noSuchOption") shouldBe Right(None)
    cfg.asEither[String]("section3", "boolOptTrue") shouldBe Right(Some("true"))
    cfg.asEither[String]("section3", "boolOptFalse") shouldBe Right(Some("false"))
  }

  it should "support asEither[Character]" in {
    val cfg = Fixture.cfg

    cfg.asEither[Character]("section3", "charOpt") shouldBe Right(Some('c'))
    cfg.asEither[Character]("section3", "intOpt").isLeft shouldBe true
    cfg.asEither[Character]("section3", "noSuchOption") shouldBe Right(None)
    cfg.asEither[Character]("section3", "boolOptTrue").isLeft shouldBe true
    cfg.asEither[Character]("section3", "boolOptFalse").isLeft shouldBe true
  }

  it should "handle unsafe substitutions" in {
    val cfg = Configuration.read(Source.fromString(Fixture.TestConfig),
                                 safe = false).get

    cfg.tryGet("section2", "substError") shouldBe Symbol("failure")
    cfg.asTry[Int]("section3", "intOpt") shouldBe Success(Some(10))
    cfg.asTry[Int]("section1", "intOpt") shouldBe Success(Some(10))
  }

  it should "detect illegal characters in section names" in {
    val cfg = Configuration.read(Source.fromString(Fixture.TestConfigWithExoticSection))
    cfg shouldBe Symbol("failure")
  }

  it should "honor given SectionNamePattern when loading data" in {
    val cfg = Configuration.read(
      Source.fromString(Fixture.TestConfigWithExoticSection),
      sectionNamePattern = """([a-zA-Z0-9_\.]+)""".r
    )
    cfg shouldBe Symbol("success")
    val loadedCfg = cfg.get
    loadedCfg.get("section1.1", "bar") shouldBe Some("baz")
  }
}

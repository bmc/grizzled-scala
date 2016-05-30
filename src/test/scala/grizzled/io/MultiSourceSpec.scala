package grizzled.io

import grizzled.BaseSpec

import scala.io.Source

class MultiSourceSpec extends BaseSpec {

  val lorem =
    """|Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
       |eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad
       |minim veniam, quis nostrud exercitation ullamco laboris nisi ut
       |aliquip ex ea commodo consequat. Duis aute irure dolor in
       |reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla
       |pariatur. Excepteur sint occaecat cupidatat non proident, sunt in
       |culpa qui officia deserunt mollit anim id est laborum.
       |""".stripMargin

  val rng = new scala.util.Random

  "MultiSource" should "handle just one wrapped Source" in {
    val src = Source.fromString(lorem)
    val ms = new MultiSource(src)
    ms.mkString shouldBe lorem
  }

  it should "handle two wrapped sources" in {
    val ms = new MultiSource(Source.fromString(lorem),
                             Source.fromString(lorem.reverse))
    ms.mkString shouldBe (lorem + lorem.reverse)
  }

  it should "handle N wrapped sources" in {
    val strings = randomStrings(100, 100, 1024)
    val ms = new MultiSource(strings.map(Source.fromString): _*)
    ms.mkString shouldBe strings.mkString
  }

  it should "allow resetting of resettable Sources" in {
    val strings = randomStrings(50, 100, 2000)
    val ms = new MultiSource(strings.map(Source.fromString): _*)
    ms.mkString shouldBe strings.mkString
    val ms2 = ms.reset()
    ms2.mkString shouldBe strings.mkString
  }

  def randomStrings(totalStrings: Int, lower: Int, upper: Int): List[String] = {
    (1 to totalStrings).map { i =>
      val total = rng.nextInt(upper - lower + 1) + lower
      (1 to total).map(_ => rng.nextPrintableChar()).mkString
    }
    .toList
  }
}

package grizzled.parsing

import grizzled.BaseSpec

class PushbackSpec extends BaseSpec {

  "Pushback" should "push back a single item" in {
    val p = new SafeIterator[Int](1 to 10) with Pushback[Int]
    p.next shouldBe Some(1)
    p.pushback(1)
    p.next shouldBe Some(1)
    p.next shouldBe Some(2)
  }

  it should "push back multiple items" in {
    val p = new SafeIterator[String]((1 to 10).map(_.toString)) with Pushback[String]
    p.next shouldBe Some("1")
    p.next shouldBe Some("2")
    p.pushback(List("1", "2"))
    p.next shouldBe Some("1")
    p.next shouldBe Some("2")
  }

  it should "push back items that weren't there originally" in {
    val p = new SafeIterator[Int](1 to 10) with Pushback[Int]
    p.next shouldBe Some(1)
    p.pushback(100)
    p.next shouldBe Some(100)
    p.next shouldBe Some(2)
  }

  it should "allow arbitrarily large pushback" in {
    val p = new SafeIterator[Int](50 to 60) with Pushback[Int]
    p.pushback((1 to 49).toList)
    for (i <- 1 to 60)
      p.next shouldBe Some(i)

    p.next shouldBe None
  }
}

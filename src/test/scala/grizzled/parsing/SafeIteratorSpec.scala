package grizzled.parsing

import grizzled.BaseSpec

class SafeIteratorSpec extends BaseSpec {
  "SafeIterator" should "iterate properly over a wrapped iterator" in {
    val i = SafeIterator((1 to 10).toIterator)
    for (j <- 1 to 10)
      i.next shouldBe Some(j)
  }

  it should "iterate properly over a wrapped iterable" in {
    val i = SafeIterator((1 to 10).map(_.toString))
    for (j <- 1 to 10)
      i.next shouldBe Some(j.toString)
  }

  it should "repeatedly return None when the underlying iterator is exhausted" in {
    val i = SafeIterator(Array(1))
    i.next shouldBe Some(1)
    i.next shouldBe None
    i.next shouldBe None
  }
}

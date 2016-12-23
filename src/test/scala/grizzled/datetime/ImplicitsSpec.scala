package grizzled.datetime

import java.sql.Timestamp
import java.util.Date

import grizzled.BaseSpec

/**
  */
class ImplicitsSpec extends BaseSpec {

  val Deltas = Array(10000000L, -238947233L, 10L, -2987234987L)

  "EnrichedTimestamp" should "convert a Timestamp to a Calendar" in {
    import Implicits.EnrichedTimestamp

    for (delta <- Deltas) {
      val date = new Date(System.currentTimeMillis + delta)
      val ts = new Timestamp(date.getTime)
      val cal = ts.toCalendar
      date shouldBe cal.getTime
    }
  }

  it should "convert a Timestamp to a Date" in {
    import Implicits.EnrichedTimestamp

    for (delta <- Deltas) {
      val date = new Date(System.currentTimeMillis + delta)
      val ts = new Timestamp(System.currentTimeMillis + delta)
      ts.toDate shouldBe date
    }
  }

  "EnrichedDate" should "convert a Date to a Calendar" in {
    import Implicits.EnrichedDate

    for (delta <- Deltas) {
      val date = new Date(System.currentTimeMillis + delta)
      val cal = date.toCalendar

      date shouldBe cal.getTime
    }
  }

  "EnrichedCalendar" should "convert a Calendar to a Timestamp" in {
    import Implicits._

    for (delta <- Deltas) {
      val ts = new Timestamp(System.currentTimeMillis + delta)
      val cal = ts.toCalendar
      cal.toTimestamp shouldBe ts
    }
  }

}

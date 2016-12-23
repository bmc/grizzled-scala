package grizzled.datetime

import java.util.Date

import grizzled.BaseSpec

/** DateTimeUtil tester
  */
class DateTimeUtilSpec extends BaseSpec {
  "dateToCalendar" should "return a valid Calendar object" in {
    val date = new Date(System.currentTimeMillis - 1000000)
    val cal = DateTimeUtil.dateToCalendar(date)
    val date2 = cal.getTime

    date shouldBe date2
  }
}

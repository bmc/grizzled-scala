package grizzled.datetime

import java.util.{Calendar, Date}

/** Some basic date- and time-related helpers.
  */
object DateTimeUtil {
  /** Convert a `Date` object to a `java.util.Calendar` object.
    *
    * @param date the `Date` object
    *
    * @return the `Calendar` object
    */
  def dateToCalendar(date: Date): Calendar = {
    val cal = Calendar.getInstance
    cal.setTime(date)
    cal
  }
}

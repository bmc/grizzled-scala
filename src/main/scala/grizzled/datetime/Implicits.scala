package grizzled.datetime

import java.sql.Timestamp
import java.util.{Calendar, Date}

/** Some enrichments for various Java date- and time-related classes.
  */
object Implicits {

  /** An enriched `java.sql.Timestamp` class.
    *
    * @param ts  the `java.sql.Timestamp`
    */
  implicit class EnrichedTimestamp(ts: Timestamp) {

    /** Convert the `Timestamp` object to a `java.util.Calendar` object.
      *
      * @return the `Calendar` object
      */
    def toCalendar: Calendar = DateTimeUtil.dateToCalendar(new Date(ts.getTime))

    /** Convert the `Timestamp` object to a `java.util.Date` object.
      *
      * @return the `Date` object
      */
    def toDate: Date = new Date(ts.getTime)
  }

  /** An enriched `java.util.Date` class.
    *
    * @param date  the `java.util.Date`
    */
  implicit class EnrichedDate(date: Date) {

    /** Convert the `Date` object to a `java.util.Calendar` object.
      *
      * @return the `Calendar` object
      */
    def toCalendar: Calendar = DateTimeUtil.dateToCalendar(date)
  }

  /** An enriched `java.util.Calendar` class.
    *
    * @param cal  the `Calendar` object
    */
  implicit class EnrichedCalendar(cal: Calendar) {

    /** Convert the `Calendar` object to a `java.sql.Timestamp`.
      *
      * @return the `java.sql.Timestamp`
      */
    def toTimestamp = new Timestamp(cal.getTime.getTime)
  }
}

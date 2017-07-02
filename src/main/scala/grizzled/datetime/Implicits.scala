package grizzled.datetime

import java.sql.Timestamp
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import java.util.{Calendar, Date}

import scala.concurrent.duration.Duration

/** Some enrichments for various Java date- and time-related classes.
  */
object Implicits {

  private val NumFormatter = new DecimalFormat("#,###")

  implicit class EnhancedDuration(val duration: Duration) extends AnyVal {

    /** Return a better-formatted result than `toString`. For instance,
      * given this `Duration`:
      *
      * {{{
      *   Duration(13123, "milliseconds")
      * }}}
      *
      * the standard `toString` method will return "13123 milliseconds".
      * By contrast, `humanize` will return "13 seconds, 123 milliseconds".
      *
      * Similarly:
      *
      * {{{
      *   val d = Duration(172801003, "milliseconds")
      *
      *   d.toString     // 172801003 milliseconds
      *   d.humanize // 2 days, 1 second, 3 milliseconds
      * }}}
      *
      * @return something nicer
      */
    def humanize: String = {
      val days         = duration.toDays
      val minusDays    = duration - Duration(days, TimeUnit.DAYS)
      val hours        = minusDays.toHours
      val minusHours   = minusDays - Duration(hours, TimeUnit.HOURS)
      val minutes      = minusHours.toMinutes
      val minusMinutes = minusHours - Duration(minutes, TimeUnit.MINUTES)
      val seconds      = minusMinutes.toSeconds
      val minusSeconds = minusMinutes - Duration(seconds, TimeUnit.SECONDS)
      val millis       = minusSeconds.toMillis
      val minusMillis  = minusSeconds - Duration(millis, TimeUnit.MILLISECONDS)
      val micros       = minusMillis.toMicros
      val minusMicros  = minusMillis - Duration(micros, TimeUnit.MICROSECONDS)
      val nanos        = minusMicros.toNanos

      val units = Seq(
        (duration.toDays, "day",         "days"),
        (hours,           "hour",        "hours"),
        (minutes,         "minute",      "minutes"),
        (seconds,         "second",      "seconds"),
        (millis,          "millisecond", "milliseconds"),
        (micros,          "microsecond", "microseconds"),
        (nanos,           "nanosecond",  "nanoseconds")
      )

      val s = units.flatMap {
        case (value, _, _) if value == 0 =>
          None
        case (value, singular, _) if value == 1 =>
          Some(NumFormatter.format(value) + " " + singular)
        case (value, _, plural) =>
          Some(NumFormatter.format(value) + " " + plural)
      }
        .mkString(", ")

      if (s.isEmpty) "0 milliseconds" else s
    }
  }

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

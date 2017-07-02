package grizzled.datetime

import grizzled.BaseSpec

import scala.concurrent.duration.Duration

class EnrichedDurationSpec extends BaseSpec {
  import grizzled.datetime.Implicits.EnrichedDuration

  "humanize" should "produce valid human-readable strings" in {
    val Data = Seq(
      (Duration(1, "second"),                 "1 second"),
      (Duration(100, "seconds"),              "1 minute, 40 seconds"),
      (ms(days = 2, seconds = 1, millis = 3), "2 days, 1 second, 3 milliseconds"),
      (ms(days = 4, hours = 1, minutes = 3),  "4 days, 1 hour, 3 minutes"),
      (Duration("1.2 µs"),                    "1 microsecond, 200 nanoseconds")
    )

    for ((duration, expected) <- Data)
      duration.humanize shouldBe expected
  }

  private def ms(days:    Int = 0,
                 hours:   Int = 0,
                 seconds: Int = 0,
                 minutes: Int = 0,
                 millis:  Int = 0): Duration = {
    Duration(
      (days * 24 * 60 * 60 * 1000) +
      (hours * 60 * 60 * 1000) +
      (minutes * 60 * 1000) +
      (seconds * 1000) +
      millis,
      "milliseconds"
    )
  }
}

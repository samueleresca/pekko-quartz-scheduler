package org.apache.pekko.extension.quartz

import com.typesafe.config.ConfigFactory
import org.quartz.TriggerUtils
import org.quartz.impl.triggers.CronTriggerImpl
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.{ Calendar, TimeZone }
import scala.collection.JavaConverters._

class QuartzScheduleSpec extends AnyWordSpec with Matchers {
  "QuartzSchedule" should {
    "fetch a list of all schedules in the configuration block" in {
      schedules should have size 2
    }

    "parse out a cron schedule" in {
      schedules should contain key "cronEvery10Seconds"
      schedules("cronEvery10Seconds") shouldBe a[QuartzCronSchedule]
      val s = schedules("cronEvery10Seconds").asInstanceOf[QuartzCronSchedule]

      // build a trigger to test against
      val _t = s.buildTrigger("parseCronScheduleTest", None)

      _t shouldBe a[CronTriggerImpl]

      val t = _t.asInstanceOf[CronTriggerImpl]

      val startHour = 3
      val endHour = 9
      val numExpectedFirings = (endHour - startHour) * 60 * 6
      val firings = TriggerUtils.computeFireTimesBetween(t, null, getDate(startHour, 0), getDate(endHour, 0)).asScala

      firings should have size numExpectedFirings
    }

    "parse out a cron schedule with calendars" in {
      val calendars = QuartzCalendars(sampleCalendarConfig, TimeZone.getTimeZone("UTC"))
      val bizHoursCal = calendars("CronOnlyBusinessHours")

      schedules should contain key "cronEvery30Seconds"
      schedules("cronEvery30Seconds") shouldBe a[QuartzCronSchedule]
      val s = schedules("cronEvery30Seconds").asInstanceOf[QuartzCronSchedule]

      val _t = s.buildTrigger("parseCronScheduleTest", None)

      _t shouldBe a[CronTriggerImpl]

      val t = _t.asInstanceOf[CronTriggerImpl]

      val startHour = 3
      val endHour = 22
      val numExpectedFirings = (18 - 8) * 60 * 2
      val firings =
        TriggerUtils.computeFireTimesBetween(t, bizHoursCal, getDate(startHour, 0), getDate(endHour, 0)).asScala

      firings should have size numExpectedFirings
    }
  }

  def getDate(hour: Int, minute: Int)(implicit tz: TimeZone = TimeZone.getTimeZone("UTC")) = {
    import Calendar._
    val _day = Calendar.getInstance(tz)
    _day.set(HOUR_OF_DAY, hour) // 24 hour ... HOUR is am/pm
    _day.set(MINUTE, minute)
    _day.getTime
  }

  lazy val schedules = QuartzSchedules(sampleConfiguration, TimeZone.getTimeZone("UTC"))

  lazy val sampleCalendarConfig = {
    ConfigFactory.parseString("""
      calendars {
        CronOnlyBusinessHours {
          type = Cron
          excludeExpression = "* * 0-7,18-23 ? * *"
          timezone = "America/San_Francisco"
        }
      }
      """.stripMargin)
  }

  lazy val sampleConfiguration = {
    ConfigFactory.parseString("""
      schedules {
        cronEvery30Seconds {
          description = "A cron job that fires off every 30 seconds"
          expression = "*/30 * * ? * *"
          calendar = "CronOnlyBusinessHours"
        }
        cronEvery10Seconds {
          description = "A cron job that fires off every 10 seconds"
          expression = "*/10 * * ? * *"
        }
      }
    """.stripMargin)
  }
}

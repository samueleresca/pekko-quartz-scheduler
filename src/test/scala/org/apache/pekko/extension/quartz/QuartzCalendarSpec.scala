package org.apache.pekko.extension.quartz

import com.typesafe.config.ConfigFactory
import org.quartz.impl.calendar._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.time.LocalDate
import java.util.{ Calendar, Date, TimeZone }
import scala.collection.JavaConverters._

class QuartzCalendarSpec extends AnyFunSpec with Matchers with BeforeAndAfterAll {
  describe("Quartz Calendar configuration modelling") {
    it("should fetch a list of all calendars in a configuration block") {
      calendars.size shouldBe 7
    }

    it("should parse and create an Annual calendar") {
      calendars should contain key "WinterClosings"
      calendars("WinterClosings") shouldBe a[AnnualCalendar]
      val cal = calendars("WinterClosings").asInstanceOf[AnnualCalendar]

      import Calendar._

      cal.isDayExcluded(getCalendar(JANUARY, 1, 1995)) shouldBe true
      cal.isDayExcluded(getCalendar(JANUARY, 1, 1975)) shouldBe true
      cal.isDayExcluded(getCalendar(JANUARY, 1, 2075)) shouldBe true

      cal.isDayExcluded(getCalendar(JANUARY, 2, 1995)) shouldBe false
      cal.isDayExcluded(getCalendar(JANUARY, 2, 1975)) shouldBe false
      cal.isDayExcluded(getCalendar(JANUARY, 2, 2075)) shouldBe false

      cal.isDayExcluded(getCalendar(DECEMBER, 25, 1995)) shouldBe true
      cal.isDayExcluded(getCalendar(DECEMBER, 25, 1975)) shouldBe true
      cal.isDayExcluded(getCalendar(DECEMBER, 25, 2075)) shouldBe true

      cal.isDayExcluded(getCalendar(DECEMBER, 31, 1995)) shouldBe false
      cal.isDayExcluded(getCalendar(DECEMBER, 31, 1975)) shouldBe false
      cal.isDayExcluded(getCalendar(DECEMBER, 31, 2075)) shouldBe false
    }

    it("should parse and create a Holiday calendar") {
      calendars should contain key "Easter"
      calendars("Easter") shouldBe a[HolidayCalendar]

      val excludedDates = calendars("Easter").asInstanceOf[HolidayCalendar].getExcludedDates.asScala
      excludedDates should contain allOf (
        getDate(2013, 3, 31),
        getDate(2014, 4, 20),
        getDate(2015, 4, 5),
        getDate(2016, 3, 27),
        getDate(2017, 4, 16)
      )
    }

    it("should parse and create a Daily calendar with a standard entry") {
      calendars should contain key "HourOfTheWolf"
      calendars("HourOfTheWolf") shouldBe a[DailyCalendar]
      val cal = calendars("HourOfTheWolf").asInstanceOf[DailyCalendar]

      implicit val tz = cal.getTimeZone

      cal.toString should include("'03:00:00:000 - 05:00:00:000', inverted: false")
    }

    it("should parse and create a Monthly calendar with just one day") {
      calendars should contain key "FirstOfMonth"
      calendars("FirstOfMonth") shouldBe a[MonthlyCalendar]
      val cal = calendars("FirstOfMonth").asInstanceOf[MonthlyCalendar]

      cal.getDaysExcluded.toList should contain theSameElementsAs _monthDayRange(List(1))
    }

    it("should parse and create a Monthly calendar with multiple days") {
      calendars should contain key "FirstAndLastOfMonth"
      calendars("FirstAndLastOfMonth") shouldBe a[MonthlyCalendar]
      val cal = calendars("FirstAndLastOfMonth").asInstanceOf[MonthlyCalendar]

      cal.getDaysExcluded.toList should contain theSameElementsAs _monthDayRange(List(1, 31))
    }

    it("should parse and create a Weekly calendar with Ints for Day Names") {
      calendars should contain key "MondaysSuck"
      calendars("MondaysSuck") shouldBe a[WeeklyCalendar]
      val cal = calendars("MondaysSuck").asInstanceOf[WeeklyCalendar]

      cal.getDaysExcluded.toList should contain theSameElementsAs _weekDayRange(List(2))
    }

    it("should parse and create a Cron calendar") {
      calendars should contain key "CronOnlyBusinessHours"
      calendars("CronOnlyBusinessHours") shouldBe a[CronCalendar]
      val cal = calendars("CronOnlyBusinessHours").asInstanceOf[CronCalendar]

      cal.getCronExpression.toString shouldBe "* * 0-7,18-23 ? * *"
    }
  }

  def _monthDayRange(days: List[Int]) = (1 to 31).map { d => days contains d }

  // for some inexplicable reason WeeklyCalendar (not tested by quartz) includes day 0 also?!?!
  def _weekDayRange(days: List[Int]) = (0 to 7).map { d => days contains d }

  def getCalendar(month: Int, day: Int, year: Int)(implicit tz: TimeZone = TimeZone.getTimeZone("UTC")): Calendar = {
    val _day = Calendar.getInstance(tz)
    _day.set(year, month, day)
    _day
  }

  def getDate(year: Int, month: Int, day: Int): Date =
    Date.from(LocalDate.of(year, month, day).atStartOfDay(java.time.ZoneId.systemDefault).toInstant)

  lazy val calendars = QuartzCalendars(sampleConfiguration, TimeZone.getDefault)

  lazy val sampleConfiguration = {
    ConfigFactory.parseString("""
        calendars {
          WinterClosings {
            type = Annual
            description = "Major holiday dates that occur in the winter time every year, non-moveable (The year doesn't matter)"
            excludeDates = ["12-25", "01-01"]
          }
          Easter {
            type = Holiday
            description = "The Easter holiday (a moveable feast) for the next five years"
            excludeDates = ["2013-03-31", "2014-04-20", "2015-04-05", "2016-03-27", "2017-04-16"]
          }
          HourOfTheWolf {
            type = Daily
            description = "A period every day in which cron jobs are quiesced, during night hours"
            exclude {
              startTime = "03:00"
              endTime   = "05:00:00"
            }
            timezone = PST
          }
          FirstOfMonth {
            type = Monthly
            description = "A thinly veiled example to test monthly exclusions of one day"
            excludeDays = [1]
          }
          FirstAndLastOfMonth {
            type = Monthly
            description = "A thinly veiled example to test monthly exclusions"
            excludeDays = [1, 31]
          }
          MondaysSuck {
            type = Weekly
            description = "Everyone, including this calendar, hates mondays as an integer"
            excludeDays = [2]
            excludeWeekends = false
          }
          CronOnlyBusinessHours {
            type = Cron
            excludeExpression = "* * 0-7,18-23 ? * *"
            timezone = "America/San_Francisco"
          }
        }
      """.stripMargin)
  }
}

package org.apache.pekko.extension.quartz

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ConfigSpec extends AnyWordSpec with Matchers {

  private lazy val reference = ConfigFactory.load("reference.conf")

  "The reference configuration" should {
    "contain all default values to setup a thread pool" in {
      reference.getInt("pekko.quartz.threadPool.threadCount") mustBe 1
      reference.getInt("pekko.quartz.threadPool.threadPriority") mustBe 5
      reference.getBoolean("pekko.quartz.threadPool.daemonThreads") mustBe true
      reference.getInt("pekko.quartz.threadPool.threadCount") mustBe 1
    }

    "contain the default timezone ID" in {
      reference.getString("pekko.quartz.defaultTimezone") mustBe "UTC"
    }
  }
}

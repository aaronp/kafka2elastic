package kafka2elastic

import java.net.URL

import com.typesafe.config.ConfigFactory
import eie.io.AlphaCounter
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Span}
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._
import scala.io.Source

abstract class BaseTest
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with Eventually {
  val ids = BaseTest.counter

  def appConfig = config.getConfig("app")

  def elasticConfig = appConfig.getConfig("elastic")

  val topic = ids.next

  def config = ConfigFactory.parseString(s"""app.kafka4m.topic : test${topic}
       |app.kafka4m.consumer.topic : test${topic}
       |app.elastic.collection : collection${topic}
       |""".stripMargin).withFallback(ConfigFactory.load())

  def testTimeout: FiniteDuration = 35.seconds

  override implicit val patienceConfig = PatienceConfig(
    timeout = Span(testTimeout.toMillis, Millis))

  def waitForElasticToBeReady() = {
    val youKnowForSearch = eventually {
      Source.fromURL(new URL("http://localhost:9200")).getLines().toList
    }
    youKnowForSearch.exists(_.contains("You Know, for Search")) shouldBe true
  }
}

object BaseTest {
  val counter = AlphaCounter.from(System.currentTimeMillis())
}

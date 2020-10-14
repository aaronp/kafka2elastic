package kafka2elastic

import io.circe.Json
import kafka2elastic.Kafka2ElasticTest.EnrichedRecord
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.scalatest.GivenWhenThen

import scala.util.Success

class Kafka2ElasticTest extends BaseTest with GivenWhenThen {

  "Enrichment" should {
    "inject kafka data into elasticsearch" in {

      Given("A running kafka and elastic search")
      dockerenv.kafka().bracket {
        dockerenv.elasticSearch().bracket {

          val expectedTestDocs = 100

          When(
            s"We squirt in ${expectedTestDocs} records into kafka, topic ${topic}")
          val writeToKafka = kafka4m.writeText(appConfig)
          val numWrittenToKafka = Observable
            .range(0, expectedTestDocs)
            .map(testDataJson)
            .consumeWith(writeToKafka)
            .runToFuture
            .futureValue
          numWrittenToKafka shouldBe expectedTestDocs

          And("we run the 'Enrichment' job under test")
          waitForElasticToBeReady()
          val runningEnrichment = Kafka2Elastic(appConfig).runToFuture

          Then("We should be abe to read back our data from Elastic Search")
          val esClient = ElasticSearchClient(elasticConfig)
          val readBackFromES = eventually {
            val collection = elasticConfig.getString("collection").toLowerCase()
            val Success(results) = esClient.query(collection, "received", size = expectedTestDocs)
            results.total.toInt shouldBe expectedTestDocs
            results.toSeq.size shouldBe expectedTestDocs
            results
          }
          
          val records = readBackFromES.toSeq.map(_.as[EnrichedRecord](EnrichedRecord.codec).get)
          withClue(s"${records.mkString("\n")}") {
            records.map(_.thread).distinct.size should be > 1
            records.map(_.offset) should contain allElementsOf (0 to expectedTestDocs)
          }
        }
      }
    }
  }

  def testDataJson(i: Long): String = Hocon.toJson(
    s"""record : {
       |  mod10 : modulo${i % 10}
       |  counter : $i
       |  isEven : ${i % 2 == 0}
       |}""".stripMargin).get.noSpaces

}

object Kafka2ElasticTest {

  case class EnrichedRecord(offset: Long, partition: Long, thread: String, body: Json)

  object EnrichedRecord {
    implicit val codec = io.circe.generic.semiauto.deriveCodec[EnrichedRecord]
  }


}
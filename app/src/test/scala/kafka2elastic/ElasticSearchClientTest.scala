package kafka2elastic

import dockerenv._

import scala.util.Success

class ElasticSearchClientTest extends BaseTest {

  lazy val elasticSearch = DockerEnv.elasticSearch()

  "WriteToElastic" should {
    "be able to write to elastic" in {
      val collection = ("index" + ids.next()).toLowerCase()
      elasticSearch.bracket {
        waitForElasticToBeReady()

        val client = ElasticSearchClient(elasticConfig)
        val Success(json) = Hocon.toJson("""
            |foo : {
            |  bar : {
            |    ok : true
            |    number : 42
            |    text : "it was the best of times"
            |  }
            |  list : [1,2,3]
            |}
            |""".stripMargin)

        val document = eventually {
          val Success(doc) = client.insert(collection, json)
          doc
        }
        document._id should not be (empty)
        val Success(queryResult: ElasticSearchClient.QueryResult) =
          client.query(collection, "42")
        queryResult.total shouldBe 1
      }
    }
  }
}

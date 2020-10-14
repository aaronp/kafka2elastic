package kafka2elastic

import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import io.circe.Json
import monix.eval.Task
import monix.execution.Scheduler

/**
 * This is our application business logic - read from kafka and write to elasticsearch concurrently,
 * only committing indices back to kafka when safe to do so
 */
object Kafka2Elastic extends StrictLogging {
  private implicit val mapping = KafkaToJsonMapping

  def apply(appConfig: Config)(implicit scheduler: Scheduler): Task[Long] = {
    val esConfig = appConfig.getConfig("elastic")
    val collection = collectionFor(esConfig)
    val elastic = ElasticSearchClient(esConfig)

    val pipeline = kafka4m.loadBalance[Json, Boolean](appConfig) { json =>
      saveToElasticSearch(elastic, collection, json)
    }

    pipeline.countL
  }

  private def enrich(json: Json): Json =
    Json.obj("thread" -> Json.fromString(threadName())).deepMerge(json)

  private def threadName() = Thread.currentThread().getName

  private def collectionFor(esConfig: Config) =
    esConfig
      .getString("collection")
      .toLowerCase()
      .ensuring(_.nonEmpty, "elastic collection should not be empty")

  private def saveToElasticSearch(elastic: ElasticSearchClient,
                                  collection: String,
                                  json: Json) = {
    Task(elastic.insert(collection, enrich(json))).attempt.map(_.isRight)
  }
}

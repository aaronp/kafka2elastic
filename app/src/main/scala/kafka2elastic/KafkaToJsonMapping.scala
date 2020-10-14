package kafka2elastic

import io.circe.Json
import io.circe.parser.parse
import kafka4m.consumer.RecordDecoder
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.util.{Failure, Success, Try}

/**
  * Converts our Kafka data into json for ingestion into ES
  */
object KafkaToJsonMapping extends RecordDecoder[String, Array[Byte], Json] {
  override def decode(record: ConsumerRecord[String, Array[Byte]]): Json = {
    val json = Try(parse(new String(record.value())).toTry).flatten match {
      case Success(v) => Json.obj("message" -> v)
      case Failure(err) =>
        Json.obj(
          "error" -> Json.fromString(
            Option(err.getMessage).getOrElse(s"err:$err")))
    }

    val enriched = Json.obj(
      "key" -> Json.fromString(s"key${record.key()}"),
      "offset" -> Json.fromLong(record.offset()),
      "partition" -> Json.fromLong(record.partition),
      "body" -> json,
      "status" -> Json.fromString("received"),
    )

    enriched
  }
}

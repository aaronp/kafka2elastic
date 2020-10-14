package kafka2elastic

import com.typesafe.config.Config
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import kafka2elastic.ElasticSearchClient.{InsertResponse, QueryResult}

import scala.util.Try

/**
  * A basic elastic search client. Other elastic-search clients are available ;-)
  *
  * @param host the ES host
  */
final case class ElasticSearchClient(host: String) {

  def insert[A: Encoder](collection: String, data: A): Try[InsertResponse] = {
    val json = data.asJson.noSpaces
    val id = eie.io.MD5(json)

    val url = s"$host/$collection/_doc/$id?refresh=true"
    val response = requests.put(url,
                                data = json,
                                headers =
                                  Seq("Content-Type" -> "application/json"))
    io.circe.parser
      .parse(new String(response.bytes))
      .toTry
      .flatMap(InsertResponse.fromJson)
  }

  def query(collection: String,
            value: String,
            from: Int = 0,
            size: Int = 1000): Try[QueryResult] = {
    val url = s"$host/$collection/_search"
    val body =
      s"""{
         |  "from" : $from,
         |  "size" : $size,
         |  "query": {
         |    "multi_match" : {
         |      "query":    "${value}",
         |      "fields": [ "*" ]
         |    }
         |  }
         |}""".stripMargin
    val result = requests.post(url,
                               data = body,
                               headers =
                                 Seq("Content-Type" -> "application/json"))

    io.circe.parser
      .parse(new String(result.bytes))
      .toTry
      .flatMap(QueryResult.fromJson)
  }
}

object ElasticSearchClient {

  case class Total(value: Long)

  object Total {
    implicit val codec = io.circe.generic.semiauto.deriveCodec[Total]
  }

  case class Hit(_index: String,
                 _type: String,
                 _id: String,
                 _score: Double,
                 _source: Json) {
    def as[A: Decoder]: Try[A] = _source.as[A].toTry
  }

  object Hit {
    implicit val codec = io.circe.generic.semiauto.deriveCodec[Hit]
  }

  case class Hits(total: Total, hits: Seq[Hit])

  object Hits {
    implicit val codec = io.circe.generic.semiauto.deriveCodec[Hits]
  }

  case class QueryResult(hits: Hits) {
    def total: Long = hits.total.value

    def toSeq = hits.hits
  }

  object QueryResult {
    def fromJson(resultJson: Json) = {
      resultJson.hcursor.downField("hits").as[Hits].toTry.map(QueryResult.apply)
    }
  }

  case class InsertResponse(_seq_no: Long,
                            result: String,
                            _version: Int,
                            _id: String,
                            _index: String,
                            _type: String)

  object InsertResponse {
    implicit val codec = io.circe.generic.semiauto.deriveCodec[InsertResponse]

    def fromJson(json: Json) = json.as[InsertResponse].toTry
  }

  def apply(esConfig: Config): ElasticSearchClient = {
    val host = esConfig.getString("host")
    ElasticSearchClient(host)
  }
}
package kafka2elastic

import com.typesafe.config.{ConfigFactory, ConfigRenderOptions}
import io.circe.Json
import _root_.io.circe.parser._

import scala.util.Try

object Hocon {

  def toJson(jsonText: String): Try[Json] = {
    val jsonString = ConfigFactory
      .parseString(jsonText)
      .root
      .render(ConfigRenderOptions.concise().setJson(true))
    parse(jsonString).toTry
  }
}

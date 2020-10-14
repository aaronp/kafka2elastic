package kafka2elastic

import args4c.implicits._
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * The main entry-point.
 *
 * Specify the show=<key> command-line argument to check the configuration (or show=all to check the whole config)
 *
 * Otherwise the command-line arguments will be taken to be either <key>=<value> pairs or the location of a configuration file,
 * with precedence going from left to right:
 *
 * E.g.
 *
 * {{{
 *   Main app.elastic.collection=foo prod.conf defaults.conf
 * }}}
 *
 * @see https://porpoiseltd.co.uk/args4c/usage.html for more usage details
 *
 */
object Main {
  def main(args: Array[String]): Unit = {
    val config = args.asConfig()
    config.showIfSpecified() match {
      case Some(out) => println(out)
      case None =>
        val appConfig = config.resolve().getConfig("app")
        println(s"""
             >    +----------------+
             >    | KᗩᖴKᗩ2EᒪᗩᔕTIᑕ |
             >    +----------------+
             >
             >${appConfig.summary()}
             >""".stripMargin('>'))
        Await.result(Kafka2Elastic(appConfig).runToFuture, Duration.Inf)
    }
  }
}

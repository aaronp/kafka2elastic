import java.nio.file.Path

import eie.io._
import sbt._

object Build {

  val ProjectName = "kafka2elastic"

  object deps {

    val typesafeConfig = "com.typesafe" % "config" % "1.4.0"
    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
    val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
    val circeVersion = "0.13.0"
    val circeGenExtrasVersion = "0.13.0"
    val circe = {
      List(
        "io.circe" %% "circe-generic" % circeVersion,
        "io.circe" %% "circe-generic-extras" % circeGenExtrasVersion,
        "io.circe" %% "circe-parser" % circeVersion,
        "io.circe" %% "circe-literal" % circeVersion % Test
      )
    }

    val requests = List(
      "com.lihaoyi" %% "requests" % "0.6.5"
    )

    val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2" % "test"

    def testDeps = List(
      "com.github.aaronp" %% "dockerenv" % "0.5.4" % "test",
      "com.github.aaronp" %% "dockerenv" % "0.5.4" % "test" classifier "tests",
      scalaTest
    )

    def application: List[ModuleID] = {
      List(
        typesafeConfig,
        scalaLogging,
        logback,
        "com.github.aaronp" %% "kafka4m" % "0.7.3",
        "com.github.aaronp" %% "args4c" % "0.7.0",
        "com.github.aaronp" %% "eie" % "1.0.0"
      ) ++ circe ++ requests ++ testDeps
    }
  }

  def scalacSettings = {
    List(
      "-deprecation", // Emit warning and location for usages of deprecated APIs.
      "-encoding",
      "utf-8", // Specify character encoding used by source files.
      "-feature", // Emit warning and location for usages of features that should be imported explicitly.
      "-language:reflectiveCalls", // Allow reflective calls
      "-language:higherKinds", // Allow higher-kinded types
      "-language:implicitConversions", // Allow definition of implicit functions called views
      "-unchecked",
      "-language:reflectiveCalls", // Allow reflective calls
      "-language:higherKinds", // Allow higher-kinded types
      "-language:implicitConversions" // Allow definition of implicit functions called views
    )

  }

  case class DockerResources(deployResourceDir: Path, //
                             targetDir: Path) {
    def moveResourcesToDeployDir(logger: sbt.util.Logger) = {
      logger.info(
        s""" Building Docker Image with:
           |
           |   deployResourceDir = ${deployResourceDir.toAbsolutePath}
           |   targetDir         = ${targetDir.toAbsolutePath}
           |
       """.stripMargin)

      val jsDir = targetDir.resolve("web/js").mkDirs()
      IO.copyDirectory(deployResourceDir.toFile, targetDir.toFile)
      this
    }
  }


  def docker(resources: DockerResources) = {
    execIn(resources.targetDir, "docker", "build", s"--tag=${ProjectName}", ".")
  }

  def execIn(inDir: Path, cmd: String*): Unit = {
    import scala.sys.process._
    val p: ProcessBuilder = Process(cmd.toSeq, inDir.toFile)
    val retVal = p.!
    require(retVal == 0, cmd.mkString("", " ", s" in dir ${inDir} returned $retVal"))
  }
}

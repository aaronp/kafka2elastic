import sbt.{KeyRanks, project, _}

ThisBuild / organization := "com.github.aaronp"
ThisBuild / scalaVersion := "2.13.3"

val projectName = "kafka2elastic"
val username = "aaronp"
val scalaThirteen = "2.13.3"
val defaultScalaVersion = scalaThirteen

name := projectName

organization := s"com.github.$username"

scalaVersion := defaultScalaVersion

val monix =
  List("monix", "monix-execution", "monix-eval", "monix-reactive", "monix-tail")

val monixDependencies = monix.map { art =>
  "io.monix" %% art % "3.2.2"
}
libraryDependencies ++= monixDependencies ++ List(
  "com.typesafe" % "config" % "1.3.4",
  "com.github.aaronp" %% "args4c" % "0.7.0"
)

libraryDependencies ++= List(
  "org.scalactic" %% "scalactic" % "3.0.8" % "test",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "org.pegdown" % "pegdown" % "1.6.0" % "test",
  "junit" % "junit" % "4.12" % "test"
)

publishMavenStyle := true
releaseCrossBuild := true
coverageMinimum := 90
coverageFailOnMinimum := true
git.remoteRepo := s"git@github.com:$username/kafka2elastic.git"
releasePublishArtifactsAction := PgpKeys.publishSigned.value
publishConfiguration := publishConfiguration.value.withOverwrite(true)
publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)

test in assembly := {}
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)
buildInfoPackage := "k2e.build"

// see http://scalameta.org/scalafmt/
scalafmtOnCompile in ThisBuild := true
scalafmtVersion in ThisBuild := "1.4.0"

val commonSettings: Seq[Def.Setting[_]] = Seq(
  organization := s"com.github.${username}",
  scalaVersion := defaultScalaVersion,
  resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  autoAPIMappings := true,
  exportJars := false,
  crossScalaVersions := Seq(scalaThirteen),
  //libraryDependencies ++= Build.dtestDependencies,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  scalacOptions ++= Build.scalacSettings,
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := s"kafka4elastic.build",
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case str if str.contains("application.conf")  => MergeStrategy.discard
    case str if str.contains("module-info.class") => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  libraryDependencies += "com.vladsch.flexmark" % "flexmark-all" % "0.35.10" % Test,
  // see http://www.scalatest.org/user_guide/using_scalatest_with_sbt
  (testOptions in Test) += (Tests.Argument(
    TestFrameworks.ScalaTest,
    "-h",
    s"target/scalatest-reports-${name.value}",
    "-o"))
)

lazy val root = (project in file("."))
  .aggregate(
    application,
    deploy
  )

lazy val application = project
  .in(file("app"))
  .settings(commonSettings: _*)
  .settings(mainClass := Some("kafka2elastic.Main"))
  .settings(name := "app")
  .settings(libraryDependencies ++= Build.deps.application)

lazy val deploy = project
  .in(file("deploy"))
  .settings(commonSettings: _*)
  .settings(name := "deploy")
  .dependsOn(application % "compile->compile;test->test")

lazy val cloudBuild =
  taskKey[Build.DockerResources]("Prepares the app for containerisation")
    .withRank(KeyRanks.APlusTask)

cloudBuild := {
  import eie.io._
  val appAssembly = (assembly in (application, Compile)).value

  // contains the docker resources
  val deployResourceDir = (resourceDirectory in (deploy, Compile)).value.toPath

  val dockerTargetDir =
    (baseDirectory.value / "target" / "docker").toPath.mkDirs()

  val resources = Build.DockerResources(
    deployResourceDir = deployResourceDir, //
    targetDir = dockerTargetDir
  )
  resources.moveResourcesToDeployDir(sLog.value)
}

lazy val docker = taskKey[Unit]("Packages the app in a docker file")
  .withRank(KeyRanks.APlusTask)

// see https://docs.docker.com/engine/reference/builder
docker := {
  Build.docker(cloudBuild.value)
}

// see http://www.scalatest.org/user_guide/using_scalatest_with_sbt
testOptions in Test += (Tests
  .Argument(TestFrameworks.ScalaTest, "-h", s"target/scalatest-reports", "-oN"))

pomExtra := {
  <url>https://github.com/
    {username}
    /
    {projectName}
  </url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <developers>
      <developer>
        <id>
          {username}
        </id>
        <name>
          {username}
        </name>
        <url>http://github.com/
          {username}
        </url>
      </developer>
    </developers>
}

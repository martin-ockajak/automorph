// Repository
val projectRoot = "org"
val projectName = "automorph"
val projectDescription = "RPC client and server for Scala"
val repositoryPath = s"martin-ockajak/$projectName"
val repositoryUrl = s"https://github.com/$repositoryPath"
val repositoryShell = s"git@github.com:$repositoryPath.git"
val siteUrl = s"https://$projectName.$projectRoot"

// Metadata
ThisBuild / homepage := Some(url(siteUrl))
ThisBuild / licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / description := projectDescription
ThisBuild / organization := s"$projectRoot.$projectName"
ThisBuild / organizationName := projectName
ThisBuild / organizationHomepage := Some(url(siteUrl))
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
//apiURL := Some(url(s"$siteUrl/api"))

ThisBuild / developers := List(Developer(
  id = "m",
  name = "Martin Ockajak",
  email = "automorph.release@gmail.com",
  url = url(s"https://github.com/martin-ockajak"),
))
Global / onChangedBuildSource := ReloadOnSourceChanges
onLoadMessage := ""

// Structure
lazy val root = project.in(file(".")).settings(name := projectName, publish / skip := true).aggregate(
  // Common
  spi,
  util,
  coreMeta,
  core,

  // Specification
  openrpc,
  openapi,

  // RPC protocol
  jsonrpcMeta,
  jsonrpc,
  webrpcMeta,
  webrpc,

  // Transport protocol
  http,
  amqp,

  // Message codec
  circe,
  jackson,
  upickle,
  argonaut,

  // Effect system
  standard,
  zio,
  monix,
  catsEffect,
  scalazEffect,

  // Message transport
  sttp,
  undertow,
  vertx,
  jetty,
  akkaHttp,
  finagle,
  rabbitmq,

  // Misc
  default,
  examples,
)

// Dependencies
def source(project: Project, path: String, dependsOn: ClasspathDep[ProjectReference]*): Project = {
  val sourceDependency = project.in(file(path)).dependsOn(dependsOn: _*)
  path.split('/') match {
    case Array("test", _ @_*) => sourceDependency
    case Array("examples", _ @_*) => sourceDependency
    case Array(_, directories @ _*) => sourceDependency.settings(
        name := s"$projectName-${directories.mkString("-")}",
        Compile / doc / scalacOptions :=
          scala3ScalacOptions ++ Seq(s"-source-links:src=github://$repositoryPath/master"),
      )
  }
}

// Common
lazy val spi = source(project, "common/spi").settings(libraryDependencies ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) => Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value)
    case _ => Seq.empty
  }
})
lazy val util = source(project, "common/util", spi)
  .settings(libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.36")
lazy val coreMeta = source(project, "common/core/meta", spi, util)
lazy val core = source(project, "common/core", coreMeta, testPlugin % Test, jsonrpc % Test, webrpc % Test)

// Specification
lazy val openrpc = source(project, "schema/openrpc", spi, testBase % Test)
lazy val openapi = source(project, "schema/openapi", spi, testBase % Test)

// Protocol
lazy val jsonrpcMeta = source(project, "protocol/jsonrpc/meta", spi)
lazy val jsonrpc = source(project, "protocol/jsonrpc", jsonrpcMeta, openrpc, openapi, util)
lazy val webrpcMeta = source(project, "protocol/webrpc/meta", spi, http)
lazy val webrpc = source(project, "protocol/webrpc", webrpcMeta, openapi, util)

// Effect system
lazy val standard = source(project, "system/standard", core, http, testCore % Test, testHttp % Test)
lazy val zio = source(project, "system/zio", spi, testStandard % Test)
  .settings(libraryDependencies += "dev.zio" %% "zio" % "1.0.18")
lazy val monix = source(project, "system/monix", spi, testStandard % Test)
  .settings(libraryDependencies += "io.monix" %% "monix-eval" % "3.4.1")
lazy val catsEffect = source(project, "system/cats-effect", spi, testStandard % Test)
  .settings(libraryDependencies += "org.typelevel" %% "cats-effect" % "3.4.6")
lazy val scalazEffect = source(project, "system/scalaz-effect", spi, testStandard % Test)
  .settings(libraryDependencies += "org.scalaz" %% "scalaz-effect" % "7.4.0-M13")

// Message codec
val circeVersion = "0.14.4"
lazy val circe = source(project, s"codec/circe", jsonrpc, webrpc, testPlugin % Test).settings(
  libraryDependencies ++= Seq("io.circe" %% "circe-parser" % circeVersion, "io.circe" %% "circe-generic" % circeVersion)
)
val jacksonVersion = "2.14.2"
lazy val jackson = source(project, "codec/jackson", jsonrpc, webrpc, testPlugin % Test).settings(
  libraryDependencies += ("com.fasterxml.jackson.module" % "jackson-module-scala" % jacksonVersion)
    .cross(CrossVersion.for3Use2_13)
)
lazy val upickle = source(project, "codec/upickle", jsonrpc, webrpc, testPlugin % Test)
  .settings(libraryDependencies += "com.lihaoyi" %% "upickle" % "2.0.0")
lazy val argonaut = source(project, "codec/argonaut", jsonrpc, webrpc, testPlugin % Test)
  .settings(libraryDependencies += "io.argonaut" %% "argonaut" % "6.3.8")

// Message transport
lazy val http = source(project, "transport/http", jsonrpc)
lazy val amqp = source(project, "transport/amqp")
val sttpVersion = "3.3.18"
val sttpHttpClientVersion = "3.3.18"
lazy val sttp = source(project, "transport/sttp", core, http, testStandard % Test).settings(
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % sttpVersion % Test,
    "com.softwaremill.sttp.client3" %% "httpclient-backend" % sttpHttpClientVersion % Test,
  )
)
lazy val rabbitmq = source(project, "transport/rabbitmq", amqp, core, standard, testCore % Test, testAmqp % Test)
  .settings(libraryDependencies += "com.rabbitmq" % "amqp-client" % "5.16.0")

// Server
lazy val undertow = source(project, "transport/undertow", core, http, testStandard % Test).settings(
  libraryDependencies += "io.undertow" % "undertow-core" % "2.3.3.Final",
  Test / javaOptions += "-Dorg.jboss.logging.provider=slf4j",
  Test / fork := true,
)
lazy val vertx = source(project, "transport/vertx", core, http, testStandard % Test)
  .settings(libraryDependencies += "io.vertx" % "vertx-core" % "4.3.8")
val jettyVersion = "11.0.13"
lazy val jetty = source(project, "transport/jetty", core, http, testStandard % Test).settings(
  libraryDependencies ++= Seq(
    "org.eclipse.jetty.websocket" % "websocket-jetty-client" % jettyVersion,
    "org.eclipse.jetty" % "jetty-servlet" % jettyVersion,
  )
)
lazy val akkaHttp = source(project, "transport/akka-http", core, http, testStandard % Test).settings(
  libraryDependencies ++= Seq(
    ("com.typesafe.akka" %% "akka-http" % "10.4.0").cross(CrossVersion.for3Use2_13),
    ("com.typesafe.akka" %% "akka-actor-typed" % "2.7.0").cross(CrossVersion.for3Use2_13),
    ("com.typesafe.akka" %% "akka-stream" % "2.7.0").cross(CrossVersion.for3Use2_13),
  )
)
lazy val finagle = source(project, "transport/finagle", core, http, testStandard % Test).settings(
  libraryDependencies += ("com.twitter" % "finagle-http" % "22.12.0")
    .exclude("org.scala-lang.modules", "scala-collection-compat_2.13").cross(CrossVersion.for3Use2_13)
)

// Miscellaneous
lazy val default = project.dependsOn(jsonrpc, circe, standard, undertow, testStandard % Test).settings(
  name := s"$projectName-default",
  libraryDependencies += "com.softwaremill.sttp.client3" %% "httpclient-backend" % sttpHttpClientVersion,
)
lazy val examples = source(project, "examples", default, upickle, zio, testPlugin % Test).settings(
  libraryDependencies += "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpVersion % Test,
  Compile / scalaSource := baseDirectory.value / "project/src/main/scala",
  Test / scalaSource :=
    (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => baseDirectory.value / "project/src/test/cala"
      case _ => baseDirectory.value / "project/src/test/scala-2"
    }),
  Test / parallelExecution := false,
)

// Test
val scribeVersion = "3.11.0"
ThisBuild / Test / testOptions += Tests.Argument("-oD")
lazy val testBase = source(project, "test/base", spi).settings(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.15",
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.11.0",
    "com.outr" %% "scribe-file" % scribeVersion,
    "com.outr" %% "scribe-slf4j" % scribeVersion,
    "com.lihaoyi" %% "pprint" % "0.8.1",
  )
)
lazy val testPlugin = source(project, "test/plugin", testBase)
  .settings(libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion)
lazy val testCore = source(project, "test/core", testPlugin, core, http, circe, jackson, upickle, argonaut)
lazy val testHttp = source(project, "test/http", testBase, http)
lazy val testAmqp = source(project, "test/amqp", testBase, amqp)
lazy val testStandard = source(project, "test/standard", testCore, testHttp, standard)

// Compile
ThisBuild / scalaVersion := "3.2.2"
ThisBuild / crossScalaVersions += "2.13.10"
ThisBuild / javacOptions ++= Seq("-source", "11", "-target", "11")

val commonScalacOptions =
  Seq("-language:higherKinds", "-feature", "-deprecation", "-unchecked", "-release", "9", "-encoding", "utf8")
val scala3ScalacOptions = commonScalacOptions ++ Seq("-language:adhocExtensions", "-pagewidth", "120")

ThisBuild / scalacOptions ++=
  (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((3, _)) => scala3ScalacOptions ++ Seq("-source", "3.2", "-indent", "-Xcheck-macros", "-Ysafe-init")
    case _ => commonScalacOptions ++ Seq(
        "-language:existentials",
        "-J--add-modules",
        "-Jjava.net.http",
        "-Xsource:3",
        "-Xlint",
        "-Wconf:site=[^.]+\\.codec\\.json\\..*:silent,cat=other-non-cooperative-equals:silent",
        "-Wextra-implicit",
        "-Wnumeric-widen",
        "-Wvalue-discard",
        "-Wunused:imports,patvars,privates,locals,params",
        "-Vfree-terms",
        "-Vimplicits",
        "-Ybackend-parallelism",
        "4",
      )
  })

// Analyze
scalastyleConfig := baseDirectory.value / "project/scalastyle-config.sbt.xml"
Compile / scalastyleSources ++= (Compile / unmanagedSourceDirectories).value
scalastyleFailOnError := true
lazy val testScalastyle = taskKey[Unit]("testScalastyle")
testScalastyle := (Test / scalastyle).toTask("").value
Test / test := (Test / test).dependsOn(testScalastyle).value

// Documentation
def flattenTasks[A](tasks: Seq[Def.Initialize[Task[A]]]): Def.Initialize[Task[Seq[A]]] =
  tasks match {
    case Seq() => Def.task(Seq())
    case Seq(head, tail @ _*) => Def.taskDyn(flattenTasks(tail).map(_.+:(head.value)))
  }

lazy val allSources = Def.taskDyn(flattenTasks(root.uses.map(_ / Compile / doc / sources)))
lazy val allTastyFiles = Def.taskDyn(flattenTasks(root.uses.map(_ / Compile / doc / tastyFiles)))
lazy val allDependencyClasspath = Def.taskDyn(flattenTasks(root.uses.map(_ / Compile / doc / dependencyClasspath)))
lazy val docs = project.in(file("site")).settings(
  name := projectName,
  mdocVariables := Map("PROJECT_VERSION" -> version.value, "SCALADOC_VERSION" -> "3.2.2"),
  mdocOut := baseDirectory.value / "docs",
  mdocExtraArguments := Seq("--no-link-hygiene"),
  mdoc / fileInputs ++= Seq(
    (LocalRootProject / baseDirectory).value.toGlob / "docs" / ** / "*.md",
    (LocalRootProject / baseDirectory).value.toGlob / "docs" / ** / "*.jpg",
  ),
  Compile / doc / scalacOptions := scala3ScalacOptions ++ Seq(s"-source-links:src=github://$repositoryPath/master"),
  Compile / doc / sources ++= allSources.value.flatten,
  Compile / doc / tastyFiles ++= allTastyFiles.value.flatten.filter(_.getName != "MonixSystem.tasty"),
  Compile / doc / dependencyClasspath ++=
    allDependencyClasspath.value.flatten.filter(_.data.getName != "cats-effect_3-2.5.4.jar"),
).enablePlugins(MdocPlugin)

// Site
val site = taskKey[Unit]("Generates project website.")

site := {
  import scala.sys.process.Process
  (docs / Compile / doc).value
  (docs / mdoc).toTask("").value
  IO.copyDirectory(
    (examples / baseDirectory).value / "project",
    (docs / baseDirectory).value / "static/examples/project",
    overwrite = true,
  )
  Process(Seq("yarn", "install"), (docs / baseDirectory).value).!
  Process(Seq("yarn", "build"), (docs / baseDirectory).value, "SITE_DOCS" -> "docs").!
  IO.copyDirectory((docs / Compile / doc / target).value, (docs / baseDirectory).value / "build/api", overwrite = true)
  val systemApiSuffix = "automorph/system"
  val monixApiDirectory = (monix / Compile / doc / target).value / systemApiSuffix
  IO.listFiles(monixApiDirectory).foreach { file =>
    IO.copyFile(file, (docs / baseDirectory).value / "build/api" / systemApiSuffix / file.name)
  }
}
val serveSite = taskKey[Unit]("Continuously generates project website.")

serveSite := {
  import scala.sys.process.Process
  Process(Seq("yarn", "start"), (docs / baseDirectory).value, "SITE_DOCS" -> "docs").!
}
serveSite := serveSite.dependsOn(site).value

cleanFiles ++= Seq(
  (docs / baseDirectory).value / "build",
  (docs / baseDirectory).value / "docs",
  (docs / baseDirectory).value / "static/examples",
)

// Deployment
enablePlugins(GhpagesPlugin)
siteSourceDirectory := (docs / baseDirectory).value / "build"
git.remoteRepo := repositoryShell
val deploySite = taskKey[Unit]("Deploys project website.")
deploySite := {}
deploySite := deploySite.dependsOn(site, ghpagesPushSite).value

// Release
ThisBuild / releaseCrossBuild := false
ThisBuild / scmInfo := Some(ScmInfo(url(repositoryUrl), s"scm:$repositoryShell"))
ThisBuild / releaseVcsSign := true
ThisBuild / releasePublishArtifactsAction := PgpKeys.publishSigned.value
ThisBuild / versionScheme := Some("semver-spec")
credentials += Credentials("GnuPG Key ID", "gpg", "1735B0FD9A286C8696EB5E6117F23799295F187F", "")
credentials += Credentials(Path.userHome / ".sbt/sonatype_credentials")

// Project

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
ThisBuild / licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / description := projectDescription
ThisBuild / organization := s"$projectRoot.$projectName"
ThisBuild / organizationName := projectName
ThisBuild / organizationHomepage := Some(url(siteUrl))
ThisBuild / developers := List(
  Developer(
    id = "m",
    name = "Martin Ockajak",
    email = "automorph.release@gmail.com",
    url = url(s"https://github.com/martin-ockajak")
  )
)
Global / onChangedBuildSource := ReloadOnSourceChanges
onLoadMessage := ""

// Structure
lazy val root = project.in(file(".")).settings(
  name := projectName,
  publish / skip := true
).aggregate(
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
  restrpcMeta,
  restrpc,

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
  tapir,
  undertow,
  jetty,
  finagle,
  rabbitmq,

  // Misc
  default,
  examples
)

// Dependencies

// Common
lazy val spi = project.in(file("common/spi")).settings(
  name := s"$projectName-spi"
).settings(
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(
	"org.scala-lang" % "scala-reflect" % scalaVersion.value
      )
      case _ => Seq.empty
    }
  }
)
lazy val util = project.in(file("common/util")).dependsOn(
  spi
).settings(
  name := s"$projectName-util",
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-api" % "1.7.32"
  )
)
lazy val coreMeta = project.in(file("common/core/meta")).dependsOn(
  spi, util
).settings(
  name := s"$projectName-core-meta",
  initialize ~= { _ =>
//    System.setProperty("macro.debug", "true")
    System.setProperty("macro.test", "true")
  },
  Compile / doc / scalacOptions ++= Seq(
    "-Ymacro-expand:none",
    "-skip-packages automorph.client.meta:automorph.handler.meta"
  )
)
lazy val core = project.in(file("common/core")).dependsOn(
  coreMeta, testPlugin % Test, jsonrpc % Test, restrpc % Test
).settings(
  name := s"$projectName-core",
  Compile / doc / scalacOptions ++= Seq(
    "-Ymacro-expand:none",
    "-skip-packages automorph.handler.meta"
  )
)

// Specification
lazy val openrpc = project.in(file("schema/openrpc")).dependsOn(
  spi, testBase % Test
).settings(
  name := s"$projectName-openrpc"
)
lazy val openapi = project.in(file("schema/openapi")).dependsOn(
  spi, testBase % Test
).settings(
  name := s"$projectName-openapi"
)

// Protocol
lazy val jsonrpcMeta = project.in(file("protocol/jsonrpc/meta")).dependsOn(
  spi
).settings(
  name := s"$projectName-jsonrpc-meta"
)
lazy val jsonrpc = project.in(file("protocol/jsonrpc")).dependsOn(
  jsonrpcMeta, openrpc, openapi, util
).settings(
  name := s"$projectName-jsonrpc"
)
lazy val restrpcMeta = project.in(file("protocol/restrpc/meta")).dependsOn(
  spi, http
).settings(
  name := s"$projectName-restrpc-meta"
)
lazy val restrpc = project.in(file("protocol/restrpc")).dependsOn(
  restrpcMeta, openapi, util
).settings(
  name := s"$projectName-restrpc"
)

// Effect system
lazy val standard = project.in(file(s"system/standard")).dependsOn(
  core, http, testCore % Test, testHttp % Test
).settings(
  name := s"$projectName-standard"
)
lazy val zio = project.in(file("system/zio")).dependsOn(
  spi, testStandard % Test
).settings(
  name := s"$projectName-zio",
  libraryDependencies ++= Seq(
    "dev.zio" %% "zio" % "1.0.12"
  ),
  Compile / doc / scalacOptions ++= Seq("-skip-packages zio")
)
lazy val monix = project.in(file("system/monix")).dependsOn(
  spi, testStandard % Test
).settings(
  name := s"$projectName-monix",
  libraryDependencies ++= Seq(
    "io.monix" %% "monix-eval" % "3.4.0"
  )
)
lazy val catsEffect = project.in(file("system/cats-effect")).dependsOn(
  spi, testStandard % Test
).settings(
  name := s"$projectName-cats-effect",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "3.2.9"
  )
)
lazy val scalazEffect = project.in(file("system/scalaz-effect")).dependsOn(
  spi, testStandard % Test
).settings(
  name := s"$projectName-scalaz-effect",
  libraryDependencies ++= Seq(
    "org.scalaz" %% "scalaz-effect" % "7.4.0-M8"
  )
)

// Message codec
val circeVersion = "0.14.1"
lazy val circe = project.in(file(s"codec/circe")).dependsOn(
  jsonrpc, restrpc, testPlugin % Test
).settings(
  name := s"$projectName-circe",
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion
  ),
  Compile / doc / scalacOptions ++= Seq(
    "-Ymacro-expand:none",
    "-skip-packages automorph.codec.json.meta"
  )
)
val jacksonVersion = "2.13.0"
lazy val jackson = project.in(file("codec/jackson")).dependsOn(
  jsonrpc, restrpc, testPlugin % Test
).settings(
  name := s"$projectName-jackson",
  libraryDependencies ++= Seq(
    ("com.fasterxml.jackson.module" % "jackson-module-scala" % jacksonVersion).cross(CrossVersion.for3Use2_13)
  )
)

lazy val upickle = project.in(file("codec/upickle")).dependsOn(
  jsonrpc, restrpc, testPlugin % Test
).settings(
  name := s"$projectName-upickle",
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "upickle" % "1.4.2"
  ),
  Compile / doc / scalacOptions ++= Seq(
    "-Ymacro-expand:none",
    "-skip-packages automorph.codec.json.meta:automorph.codec.messagepack.meta"
  )
)
lazy val argonaut = project.in(file("codec/argonaut")).dependsOn(
  jsonrpc, restrpc, testPlugin % Test
).settings(
  name := s"$projectName-argonaut",
  libraryDependencies ++= Seq(
    "io.argonaut" %% "argonaut" % "6.3.7"
  ),
  Compile / doc / scalacOptions ++= Seq(
    "-Ymacro-expand:none",
    "-skip-packages automorph.codec.json.meta"
  )
)
// Message transport
lazy val http = project.in(file("transport/http")).settings(
  name := s"$projectName-http",
).dependsOn(
  jsonrpc
)
lazy val amqp = project.in(file("transport/amqp")).settings(
  name := s"$projectName-amqp"
)
val sttpVersion = "3.3.15"
lazy val sttp = project.in(file("transport/sttp")).dependsOn(
  core, http, testStandard % Test
).settings(
  name := s"$projectName-sttp",
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
    "com.softwaremill.sttp.client3" %% "httpclient-backend" % sttpVersion % Test,
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % sttpVersion % Test
  ),
  Compile / doc / scalacOptions ++= Seq("-skip-packages sttp")
//  apiMappings += (
//    (unmanagedBase.value / s"core_3-$sttpVersion.jar") -> 
//      url(s"https://www.javadoc.io/doc/com.softwaremill.sttp.client3/core_${scalaVersion.value}/latest/")
//  )
)
lazy val rabbitmq = project.in(file("transport/rabbitmq")).dependsOn(
  amqp, core, standard, testCore % Test, testAmqp % Test
).settings(
  name := s"$projectName-rabbitmq",
  libraryDependencies ++= Seq(
    "com.rabbitmq" % "amqp-client" % "5.13.1"
  )
)

// Server
lazy val undertow = project.in(file("transport/undertow")).dependsOn(
  core, http, testStandard % Test
).settings(
  name := s"$projectName-undertow",
  libraryDependencies ++= Seq(
    "io.undertow" % "undertow-core" % "2.2.12.Final"
  )
)
val jettyVersion = "11.0.7"
lazy val jetty = project.in(file("transport/jetty")).dependsOn(
  core, http, testStandard % Test
).settings(
  name := s"$projectName-jetty",
  libraryDependencies ++= Seq(
    "org.eclipse.jetty.websocket" % "websocket-jetty-client" % jettyVersion,
    "org.eclipse.jetty" % "jetty-servlet" % jettyVersion
  )
)
lazy val finagle = project.in(file("transport/finagle")).dependsOn(
  core, http, testStandard % Test
).settings(
  name := s"$projectName-finagle",
  libraryDependencies ++= Seq(
    ("com.twitter" % "finagle-http" % "21.10.0").cross(CrossVersion.for3Use2_13)
  )
)
val tapirVersion = "0.18.3"
lazy val tapir = project.in(file("transport/tapir")).dependsOn(
  core, http, testStandard % Test
).settings(
  name := s"$projectName-tapir",
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-vertx-server" % tapirVersion % Test
  )
)

// Misc
lazy val default = project.dependsOn(
  jsonrpc, circe, standard, undertow, testStandard % Test
).settings(
  name := s"$projectName-default",
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client3" %% "httpclient-backend" % sttpVersion    
  ),
  Compile / doc / scalacOptions ++= Seq(
    "-Ymacro-expand:none",
    "-skip-packages automorph.meta"
  )
)
lazy val examples = project.in(file("examples")).dependsOn(
  default, upickle, zio, testPlugin % Test
).settings(
  name := s"$projectName-examples",
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpVersion % Test
  ),
  Compile / doc / scalacOptions ++= Seq("-skip-packages examples"),
  Compile / scalaSource := baseDirectory.value / "project" / "src" / "main" / "scala",
  Test / scalaSource := (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((3, _)) => baseDirectory.value / "project" / "src" / "test" / "scala"
    case _ => baseDirectory.value / "project" / "src" / "test" / "scala-2"
  }),
  Test / parallelExecution := false
)

// Test
lazy val testBase = project.in(file("test/base")).dependsOn(
  spi
).settings(
  libraryDependencies ++= Seq(
    // Test
    "org.scalatest" %% "scalatest" % "3.2.10",
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.10.0",
    "ch.qos.logback" % "logback-classic" % "1.3.0-alpha10",
    "com.lihaoyi" %% "pprint" % "0.6.6"
  ),
  Compile / doc / scalacOptions ++= Seq("-skip-packages test")
)
lazy val testPlugin = project.in(file("test/plugin")).dependsOn(
  testBase
).settings(
  libraryDependencies ++= Seq(
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion
  ),
  Compile / doc / scalacOptions ++= Seq("-skip-packages test")
)
lazy val testCore = project.in(file("test/core")).dependsOn(
  testPlugin, core, http, circe, jackson, upickle, argonaut,
).settings(
  Compile / doc / scalacOptions ++= Seq("-skip-packages test")
)
lazy val testHttp = project.in(file("test/http")).dependsOn(
  testBase, http
).settings(
  Compile / doc / scalacOptions ++= Seq("-skip-packages test")
)
lazy val testAmqp = project.in(file("test/amqp")).dependsOn(
  testBase, amqp
).settings(
  Compile / doc / scalacOptions ++= Seq("-skip-packages test")
)
lazy val testStandard = project.in(file("test/standard")).dependsOn(
  testCore, testHttp, standard
).settings(
  Compile / doc / scalacOptions ++= Seq("-skip-packages test")
)


// Compile
ThisBuild / scalaVersion := "3.0.0"
ThisBuild / crossScalaVersions ++= Seq("2.13.7", scalaVersion.value)
ThisBuild / scalacOptions ++= Seq(
  "-language:higherKinds",
  "-feature",
  "-deprecation",
  "-unchecked",
  "-release",
  "8",
  "-encoding",
  "utf8"
) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((3, _)) => Seq(
    "-source",
    "3.0",
    "-language:adhocExtensions",
    "-indent",
    "-Xcheck-macros",
    "-Ysafe-init",
    "-pagewidth",
    "120"
  )
  case _ => Seq(
    "-language:existentials",
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
    "4"
  )
})
ThisBuild / javacOptions ++= Seq(
  "-source",
  "8",
  "-target",
  "8"
)


// Analyze
scalastyleConfig := baseDirectory.value / "project" / "scalastyle.xml"
Compile / scalastyleSources ++= (Compile / unmanagedSourceDirectories).value
scalastyleFailOnError := true
lazy val testScalastyle = taskKey[Unit]("testScalastyle")
testScalastyle := (Test / scalastyle).toTask("").value
Test / test := ((Test / test).dependsOn(testScalastyle)).value


// Documentation
enablePlugins(ScalaUnidocPlugin)
lazy val placeholderDoc = project.in(file("system/doc")).dependsOn(
  spi
)
val docsDirectory = settingKey[File]("Website generator directory.")
docsDirectory := baseDirectory.value / "website"
apiURL := Some(url(s"$siteUrl/api"))
ThisBuild / autoAPIMappings := true
val allDoc = taskKey[Unit]("Generates all API documentation.")
allDoc := {
  (Compile / unidoc).value
  (catsEffect / Compile / doc).value
  val systemApiSuffix = "api/automorph/system"
  val scalaVersionSuffix = (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) => scalaVersion.value.split("\\.").init.mkString(".")
    case _ => scalaVersion.value
  })
  val catsEffectApiPath = (catsEffect / target).value / s"scala-$scalaVersionSuffix" / systemApiSuffix
  val systemApiPath = docsDirectory.value / "static" / systemApiSuffix
  IO.listFiles(catsEffectApiPath).filter(_.name != "index.html").foreach { file =>
    IO.copyFile(file, systemApiPath / file.name)
  }
}
//allDoc / fileInputs += (docsDirectory.value / "static/api").toGlob / ** / "*.html"
ScalaUnidoc / unidoc / target := docsDirectory.value / "static/api"
ScalaUnidoc / unidoc / unidocProjectFilter := inAnyProject -- inProjects(
  catsEffect
)
ScalaUnidoc / unidoc / scalacOptions ++= Seq(
  "-doc-source-url",
  scmInfo.value.get.browseUrl + "/tree/main${FILE_PATH}.scala",
  "-sourcepath",
  baseDirectory.value.getAbsolutePath,
  "-groups",
  "-Ymacro-expand:none",
  "-skip-packages",
  "test:examples:automorph.meta:automorph.client.meta:automorph.handler.meta:test:automorph.codec.json.meta:automorph.codec.messagepack.meta:zio:sttp"
)
cleanFiles += (ScalaUnidoc / unidoc / target).value
clean := clean.dependsOn(unidoc / clean).value


// Site
lazy val docs = project.in(file("website")).settings(
  mdocVariables := Map(
    "VERSION" -> version.value
  ),
  mdocOut := (LocalRootProject / docsDirectory).value / "docs",
  mdocExtraArguments := Seq("--no-link-hygiene"),
  mdoc / fileInputs ++= Seq(
    (LocalRootProject / baseDirectory).value.toGlob / "docs" / ** / "*.md",
    (LocalRootProject / baseDirectory).value.toGlob / "docs" / ** / "*.jpg"
  ),
  scalacOptions ++= Seq(
    "-Yno-imports",
    "-Xfatal-warnings"
  )
).enablePlugins(MdocPlugin)
val site = taskKey[Unit]("Generates project website.")
site := {
  import scala.sys.process.{Process, stringToProcess}
  allDoc.value
  IO.copyDirectory((examples / baseDirectory).value / "project", docsDirectory.value / "static/examples/project", true)
  (docs / mdoc).toTask("").value
  if (!(docsDirectory.value / "node_modules").exists) {
    s"yarn --cwd ${docsDirectory.value} install" !
  }
  Process(s"yarn --cwd ${docsDirectory.value} build", None, "SITE_DOCS" -> "docs") !
}
cleanFiles ++= Seq(
  docsDirectory.value / "docs",
  docsDirectory.value / "static/examples",
  docsDirectory.value / "build"
)


// Deployment
enablePlugins(GhpagesPlugin)
siteSourceDirectory := docsDirectory.value / "build"
git.remoteRepo := repositoryShell
val deploySite = taskKey[Unit]("Deploys project website.")
deploySite := {}
deploySite := deploySite.dependsOn(site, ghpagesPushSite).value


// Release
ThisBuild / releaseCrossBuild := false
ThisBuild / scmInfo := Some(ScmInfo(
  url(repositoryUrl),
  s"scm:$repositoryShell"
))
ThisBuild / releaseVcsSign := true
ThisBuild / releasePublishArtifactsAction := PgpKeys.publishSigned.value
ThisBuild / versionScheme := Some("semver-spec")
credentials += Credentials("GnuPG Key ID", "gpg", "1735B0FD9A286C8696EB5E6117F23799295F187F", "")
credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")


// Project

// Repository
val projectName = "automorph"
val repositoryPath = s"martin-ockajak/${projectName}"
val repositoryUrl = s"https://github.com/${repositoryPath}"
val documentationUrl = repositoryUrl

// Metadata
ThisBuild / organization := s"io.${name.value}"
ThisBuild / homepage := Some(url(repositoryUrl))
ThisBuild / licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / developers := List()
Global / onChangedBuildSource := ReloadOnSourceChanges

// Structure
lazy val root = project.in(file(".")).settings(
  name := projectName,
  description := "Remote procedure call client and server library for Scala ",
  publish / skip := true
).aggregate(
  // Common
  spi,
  util,
  coreMeta,
  core,
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
  upickle,
  argonaut,

  // Effect system
  standard,
  zio,
  monix,
  catsEffect,
  scalaz,

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

// Basic
lazy val spi = (project in file("common/spi")).settings(
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
lazy val util = (project in file("common/util")).settings(
  name := s"$projectName-util",
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-api" % "1.7.32"
  )
)
lazy val coreMeta = (project in file("common/core/meta")).dependsOn(
  spi, util
).settings(
  name := s"$projectName-core-meta",
  initialize ~= { _ =>
//    System.setProperty("macro.debug", "true")
    System.setProperty("macro.test", "true")
  }
)
lazy val core = (project in file("common/core")).dependsOn(
  coreMeta, testBase % Test
).settings(
  name := s"$projectName-core"
)

// Protocol
lazy val jsonrpcMeta = (project in file("protocol/jsonrpc/meta")).dependsOn(
  spi
).settings(
  name := s"$projectName-jsonrpc-meta"
)
lazy val jsonrpc = (project in file("protocol/jsonrpc")).dependsOn(
  jsonrpcMeta, util
).settings(
  name := s"$projectName-jsonrpc"
)
lazy val restrpcMeta = (project in file("protocol/restrpc/meta")).dependsOn(
  spi
).settings(
  name := s"$projectName-restrpc-meta"
)
lazy val restrpc = (project in file("protocol/restrpc")).dependsOn(
  restrpcMeta, util
).settings(
  name := s"$projectName-restrpc"
)

// Effect system
lazy val standard = (project in file(s"system/standard")).dependsOn(
  core, http, testCore % Test, testHttp % Test
).settings(
  name := s"$projectName-standard"
)
lazy val zio = (project in file("system/zio")).dependsOn(
  spi, testCore % Test
).settings(
  name := s"$projectName-zio",
  libraryDependencies ++= Seq(
    "dev.zio" %% "zio" % "1.0.10"
  )
)
lazy val monix = (project in file("system/monix")).dependsOn(
  spi, testCore % Test
).settings(
  name := s"$projectName-monix",
  libraryDependencies ++= Seq(
    "io.monix" %% "monix-eval" % "3.4.0"
  )
)
lazy val catsEffect = (project in file("system/cats-effect")).dependsOn(
  spi, testCore % Test
).settings(
  name := s"$projectName-cats-effect",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "3.2.2"
  )
)
lazy val scalaz = (project in file("system/scalaz")).dependsOn(
  spi, testCore % Test
).settings(
  name := s"$projectName-scalaz",
  libraryDependencies ++= Seq(
    "org.scalaz" %% "scalaz-effect" % "7.4.0-M7"
  )
)

// Message codec
val circeVersion = "0.14.1"
lazy val circe = (project in file(s"codec/circe")).dependsOn(
  jsonrpc, restrpc, testBase % Test
).settings(
  name := s"$projectName-circe",
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion
  )
)
lazy val upickle = (project in file("codec/upickle")).dependsOn(
  jsonrpc, restrpc, testBase % Test
).settings(
  name := s"$projectName-upickle",
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "upickle" % "1.4.0"
  )
)
lazy val argonaut = (project in file("codec/argonaut")).dependsOn(
  jsonrpc, restrpc, testBase % Test
).settings(
  name := s"$projectName-argonaut",
  libraryDependencies ++= Seq(
    "io.argonaut" %% "argonaut" % "6.3.6"
  )
)

// Message transport
lazy val http = (project in file("transport/http")).dependsOn(
  jsonrpc
)
lazy val amqp = (project in file("transport/amqp"))
val sttpVersion = "3.3.13"
lazy val sttp = (project in file("transport/sttp")).dependsOn(
  http, testCore % Test, standard % Test
).settings(
  name := s"$projectName-sttp",
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % sttpVersion % Test
  ),
//  apiMappings += (
//    (unmanagedBase.value / s"core_3-${sttpVersion}.jar") -> 
//      url("https://www.javadoc.io/doc/com.softwaremill.sttp.client3/core_2.13/latest/")
//  )
)
lazy val rabbitmq = (project in file("transport/rabbitmq")).dependsOn(
  amqp, core, standard, testCore % Test
).settings(
  name := s"$projectName-rabbitmq",
  libraryDependencies ++= Seq(
    "com.rabbitmq" % "amqp-client" % "5.13.0"
  )
)

// Server
lazy val undertow = (project in file("transport/undertow")).dependsOn(
  core, http, testCore % Test, standard % Test
).settings(
  name := s"$projectName-undertow",
  libraryDependencies ++= Seq(
    "io.undertow" % "undertow-core" % "2.2.9.Final"
  )
)
lazy val jetty = (project in file("transport/jetty")).dependsOn(
  core, http, testCore % Test, standard % Test
).settings(
  name := s"$projectName-jetty",
  libraryDependencies ++= Seq(
    "org.eclipse.jetty" % "jetty-servlet" % "11.0.6",
    "commons-io" % "commons-io" % "2.11.0"
  )
)
lazy val finagle = (project in file("transport/finagle")).dependsOn(
  core, http, testCore % Test, standard % Test
).settings(
  name := s"$projectName-finagle",
  libraryDependencies ++= Seq(
    ("com.twitter" % "finagle-http" % "21.6.0").cross(CrossVersion.for3Use2_13)
  )
)
lazy val tapir = (project in file("transport/tapir")).dependsOn(
  core, http, testCore % Test, standard % Test
).settings(
  name := s"$projectName-tapir",
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-core" % "0.18.1"
  )
)

// OpenAPI
lazy val openapi = (project in file("common/openapi")).dependsOn(
  core, circe, testCore % Test, standard % Test
).settings(
  name := s"$projectName-open-api"
)

// Misc
lazy val default = project.dependsOn(
  jsonrpc, restrpc, circe, standard, undertow, sttp
).settings(
  name := s"$projectName-default",
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % sttpVersion
  )
)
lazy val examples = (project in file("test/examples")).dependsOn(
  default, upickle, zio, testBase % Test
).settings(
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpVersion % Test
  )
)
// Test
lazy val testBase = (project in file("test/base")).dependsOn(
  spi, jsonrpc, restrpc
).settings(
  libraryDependencies ++= Seq(
    // Test
    "org.scalatest" %% "scalatest" % "3.2.9",
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0",
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )
)
lazy val testCore = (project in file("test/core")).dependsOn(
  testBase, core, jsonrpc, restrpc, http, circe, upickle, argonaut,
)
lazy val testHttp = (project in file("test/http")).dependsOn(
  testBase, http
)
lazy val testAmqp = (project in file("test/amqp")).dependsOn(
  testBase, amqp
)


// Compile
ThisBuild / scalaVersion := "3.0.0"
ThisBuild / crossScalaVersions ++= Seq("2.13.6", "3.0.0")
ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-language:higherKinds",
  "-deprecation",
  "-unchecked",
  "-release",
  "8",
  "-encoding",
  "utf8",
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
    "-Vfree-types",
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
Test / test := ((Test / test) dependsOn testScalastyle).value


// Documentation

// API
enablePlugins(ScalaUnidocPlugin)
ThisBuild / autoAPIMappings := true
apiURL := Some(url(s"https://javadoc.io/doc/${organization.value}/$projectName-core_3/latest"))
ScalaUnidoc / unidoc / scalacOptions ++= Seq(
  "-Ymacro-expand:none",
  "-groups",
  "-implicits",
  "-doc-source-url",
  scmInfo.value.get.browseUrl + "/tree/main${FILE_PATH}.scala",
  "-sourcepath",
  (LocalRootProject / baseDirectory).value.getAbsolutePath
)
//Compile / doc / scalacOptions ++= Seq("-groups", "-implicits")

// Site settings
enablePlugins(LaikaPlugin)
import laika.theme.config._
import laika.helium.config._
import laika.ast.LengthUnit._
import laika.ast.Path._
laikaTheme := laika.helium.Helium.defaults.all.metadata(
  title = Some("Automorph"),
  description = Some("Remote procedure call client and server library for Scala"),
  version = Some(version.value),
  language = Some("en")
).all.themeColors(
  primary = Color.hex("11698E"),
  secondary = Color.hex("39A2DB"),
  primaryDark = Color.hex("095269"),
  primaryMedium = Color.hex("A7D4DE"),
  primaryLight = Color.hex("EFEFEF"),
  text = Color.hex("5F3F3F")
).site.layout(
  contentWidth = vw(85),
  navigationWidth = vw(15),
  defaultBlockSpacing = px(20),
  defaultLineHeight = 1.5,
  anchorPlacement = AnchorPlacement.Right
).site.topNavigationBar(
  logo = Some(Logo.internal(
    path = Root / "images/logo.jpg", 
    alt = Some("Homepage"), 
    title = Some("Home")
  )),
  links = Seq(
    IconLink.external(repositoryUrl, HeliumIcon.github),
    IconLink.external(s"${documentationUrl}/api", HeliumIcon.api),
    IconLink.external(s"https://mvnrepository.com/artifact/${organization.value}", HeliumIcon.download)
  )
).build
laikaExtensions := Seq(laika.markdown.github.GitHubFlavor, laika.parse.code.SyntaxHighlighting)
laikaIncludeAPI := true

// Site tasks
Laika / sourceDirectories := Seq(baseDirectory.value / "doc")
val deleteSite = taskKey[Unit]("Deletes generated documentation website.")
deleteSite := {
  IO.delete((laikaSite / target).value)
}
laikaSite / target := target.value / "site"
val site = taskKey[Unit]("Generates documentation website.")
site := Def.sequential(
  deleteSite,
  Compile / doc,
  laikaSite
).value
site / fileInputs ++= Seq(
  baseDirectory.value.toGlob / "doc" / ** / "*.md",
  baseDirectory.value.toGlob / "doc" / ** / "*.conf",
  baseDirectory.value.toGlob / "doc" / ** / "*.jpg"
)

// Deployment
val repositoryShell = s"git@github.com:${repositoryPath}.git"
enablePlugins(GhpagesPlugin)
siteSourceDirectory := target.value / "site"
git.remoteRepo := repositoryShell
val deploySite = taskKey[Unit]("Deploys documentation website.")
deploySite := {}
deploySite := site.dependsOn(site, ghpagesPushSite).value


// Continuous Integration
ThisBuild / githubWorkflowPublishTargetBranches := Seq()
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / githubWorkflowPublish := Seq(WorkflowStep.Sbt(List("ci-release")))


// Release
ThisBuild / releaseCrossBuild := true
ThisBuild / scmInfo := Some(ScmInfo(
  url(repositoryUrl),
  s"scm:${repositoryShell}"
))
ThisBuild / releaseVcsSign := true
ThisBuild / releasePublishArtifactsAction := PgpKeys.publishSigned.value
ThisBuild / versionScheme := Some("semver-spec")
//ThisBuild / publishTo := Some(MavenCache("local-maven", file("target/maven-relases")))


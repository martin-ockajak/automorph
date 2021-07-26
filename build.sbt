// Project
ThisBuild / organization := "io.automorph"
ThisBuild / homepage := Some(url("https://github.com/martin-ockajak/automorph"))
ThisBuild / licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / developers := List()
ThisBuild / initialize ~= { _ =>
//  System.setProperty("macro.debug", "true")
  System.setProperty("macro.test", "true")
}

lazy val root = project.in(file(".")).aggregate(
  core,

  circe,
  upickle,
  argonaut,

  standard,
  zio,
  monix,
  catsEffect,
  scalaz,

  http,
  amqp,

  sttp,
  tapir,
  undertow,
  jetty,
  finagle,
  rabbitmq,

  openapi,
  default,

  examples
).settings(
  name := "automorph",
  description := "Remote procedure call client and server library for Scala ",
  publish / skip := true
)

// Dependencies

// Basic
lazy val spi = (project in file("common/spi")).settings(
  name := "automorph-spi"
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
lazy val meta = (project in file("common/meta")).dependsOn(
  spi
).settings(
  name := "automorph-meta",
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-api" % "1.7.31"
  )
)
lazy val core = (project in file("common/core")).dependsOn(
  meta, testBase % Test
).settings(
  name := "automorph-core"
)
lazy val standard = project.dependsOn(
  http, core, testCore % Test
).settings(
  name := "automorph-standard"
)

// Codec
val circeVersion = "0.14.1"
lazy val circe = (project in file("format/circe")).dependsOn(
  spi, testBase % Test
).settings(
  name := "automorph-circe",
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion
  )
)
lazy val upickle = (project in file("format/upickle")).dependsOn(
  spi, testBase % Test
).settings(
  name := "automorph-upickle",
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "upickle" % "1.4.0"
  )
)
lazy val argonaut = (project in file("format/argonaut")).dependsOn(
  spi, testBase % Test
).settings(
  name := "automorph-argonaut",
  libraryDependencies ++= Seq(
    "io.argonaut" %% "argonaut" % "6.3.6"
  )
)

// Effect
lazy val zio = (project in file("system/zio")).dependsOn(
  spi, testCore % Test
).settings(
  name := "automorph-zio",
  libraryDependencies ++= Seq(
    "dev.zio" %% "zio" % "1.0.9"
  )
)
lazy val monix = (project in file("system/monix")).dependsOn(
  spi, testCore % Test
).settings(
  name := "automorph-monix",
  libraryDependencies ++= Seq(
    "io.monix" %% "monix-eval" % "3.4.0"
  )
)
lazy val catsEffect = (project in file("system/cats-effect")).dependsOn(
  spi, testCore % Test
).settings(
  name := "automorph-cats-effect",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "3.1.1"
  )
)
lazy val scalaz = (project in file("system/scalaz")).dependsOn(
  spi, testCore % Test
).settings(
  name := "automorph-scalaz",
  libraryDependencies ++= Seq(
    "org.scalaz" %% "scalaz-effect" % "7.4.0-M7"
  )
)

// Transport
val sttpVersion = "3.3.11"
lazy val http = (project in file("transport/http"))
lazy val amqp = (project in file("transport/amqp"))
lazy val sttp = (project in file("transport/sttp")).dependsOn(
  http, core, testCore % Test, standard % Test
).settings(
  name := "automorph-sttp",
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
  amqp, core, testCore % Test, standard % Test
).settings(
  name := "automorph-rabbitmq",
  libraryDependencies ++= Seq(
    "com.rabbitmq" % "amqp-client" % "5.13.0"
  )
)

// Server
lazy val undertow = (project in file("transport/undertow")).dependsOn(
  http, core, testCore % Test, standard % Test
).settings(
  name := "automorph-undertow",
  libraryDependencies ++= Seq(
    "io.undertow" % "undertow-core" % "2.2.9.Final",
    "com.lihaoyi" %% "cask" % "0.7.11" % Test
  )
)
lazy val jetty = (project in file("transport/jetty")).dependsOn(
  http, core, testCore % Test, standard % Test
).settings(
  name := "automorph-jetty",
  libraryDependencies ++= Seq(
    "org.eclipse.jetty" % "jetty-servlet" % "11.0.6",
    "commons-io" % "commons-io" % "2.11.0"
  )
)
lazy val finagle = (project in file("transport/finagle")).dependsOn(
  http, core, testCore % Test, standard % Test
).settings(
  name := "automorph-finagle",
  libraryDependencies ++= Seq(
    ("com.twitter" % "finagle-http" % "21.6.0").cross(CrossVersion.for3Use2_13)
  )
)
lazy val tapir = (project in file("transport/tapir")).dependsOn(
  http, core, testCore % Test, standard % Test
).settings(
  name := "automorph-tapir",
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-core" % "0.18.0"
  )
)

// OpenAPI
lazy val openapi = (project in file("common/openapi")).dependsOn(
  core, circe
).settings(
  name := "automorph-open-api"
)

// Default
lazy val default = project.dependsOn(
  circe, standard, undertow, sttp
).settings(
  name := "automorph-default",
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % sttpVersion
  )
)

// Examples
lazy val examples = (project in file("test/examples")).dependsOn(
  default, upickle, zio, testBase % Test
).settings(
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpVersion
  )
)
// Test
lazy val testBase = (project in file("test/base")).dependsOn(
  spi
).settings(
  libraryDependencies ++= Seq(
    // Test
    "org.scalatest" %% "scalatest" % "3.2.9",
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0",
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )
)
lazy val testCore = (project in file("test/core")).dependsOn(
  testBase, core, upickle, circe, argonaut,
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
    "-Xlint",
    "-Wconf:site=[^.]+\\.format\\.json\\..*:silent,cat=other-non-cooperative-equals:silent",
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
apiURL := Some(url("https://javadoc.io/doc/io.automorph/automorph-core_3/latest"))
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

// Site
enablePlugins(LaikaPlugin)
import laika.helium.config._
import laika.ast.LengthUnit._
//laikaTheme := laika.helium.Helium.defaults.site.layout(
//  contentWidth = vw(60),
//  navigationWidth = vw(40),
//  defaultBlockSpacing = px(10),
//  defaultLineHeight = 1.5,
//  anchorPlacement = AnchorPlacement.Right
//).build
laikaTheme := laika.theme.Theme.empty
laikaExtensions := Seq(laika.markdown.github.GitHubFlavor, laika.parse.code.SyntaxHighlighting)
laikaIncludeAPI := true
Laika / sourceDirectories := Seq(baseDirectory.value / "doc")
laikaSite / target := target.value / "site"
val site = taskKey[Unit]("Generates documentation website.")
site := {}
site := site.dependsOn(laikaSite).value

// Deployment
enablePlugins(GhpagesPlugin)
siteSourceDirectory := target.value / "site"
git.remoteRepo := "git@github.com:martin-ockajak/automorph.git"
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
  url("https://github.com/martin-ockajak/automorph"),
  "scm:git:git@github.com/martin-ockajak/automorph.git"
))
ThisBuild / releaseVcsSign := true
ThisBuild / releasePublishArtifactsAction := PgpKeys.publishSigned.value
ThisBuild / versionScheme := Some("semver-spec")
//ThisBuild / publishTo := Some(MavenCache("local-maven", file("target/maven-relases")))


// Project
ThisBuild / organization := "io.json-rpc"

lazy val root = project.in(file(".")).aggregate(
  core,

  upickle,
  circe,
  argonaut,

  standard,
  zio,
  monix,
  cats,
  scalaz,

  sttp,

  undertow,
  finagle
).settings(
  name := "json-rpc",
  description := "JSON-RPC client & server",
  publish / skip := true,
//  crossScalaVersions := Seq.empty
).enablePlugins(ScalaUnidocPlugin)


// Dependencies

// Basic
lazy val spi = project.settings(
  name := "json-rpc-spi"
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
lazy val coreMeta = (project in file("core/meta")).dependsOn(
  spi
)
lazy val core = project.dependsOn(
  coreMeta, testBase % Test
).settings(
  name := "json-rpc-core",
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-api" % "1.7.31"
  ),
  Compile / packageBin / mappings ++= (coreMeta / Compile / packageBin / mappings).value,
  Compile / packageSrc / mappings ++= (coreMeta / Compile / packageSrc / mappings).value
)
lazy val standard = project.dependsOn(
  core, testCore % Test
).settings(
  name := "json-rpc-standard"
)

// Codec
lazy val upickle = (project in file("codec/upickle")).dependsOn(
  spi, testBase % Test
).settings(
  name := "json-rpc-upickle",
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "upickle" % "1.4.0"
  )
)
lazy val circe = (project in file("codec/circe")).dependsOn(
  spi, testBase % Test
).settings(
  name := "json-rpc-circe",
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-parser" % "0.14.1",
    "io.circe" %% "circe-generic" % "0.14.1"
  )
)
lazy val argonaut = (project in file("codec/argonaut")).dependsOn(
  spi, testBase % Test
).settings(
  name := "json-rpc-argonaut",
  libraryDependencies ++= Seq(
    "io.argonaut" %% "argonaut" % "6.3.5"
  )
)

// Effect
lazy val zio = (project in file("backend/zio")).dependsOn(
  spi, testCore % Test
).settings(
  name := "json-rpc-zio",
  libraryDependencies ++= Seq(
    "dev.zio" %% "zio" % "1.0.9"
  )
)
lazy val monix = (project in file("backend/monix")).dependsOn(
  spi, testCore % Test
).settings(
  name := "json-rpc-monix",
  libraryDependencies ++= Seq(
    "io.monix" %% "monix-eval" % "3.4.0"
  )
)
lazy val cats = (project in file("backend/cats")).dependsOn(
  spi, testCore % Test
).settings(
  name := "json-rpc-cats",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "3.1.1"
  )
)
lazy val scalaz = (project in file("backend/scalaz")).dependsOn(
  spi, testCore % Test
).settings(
  name := "json-rpc-scalaz",
  libraryDependencies ++= Seq(
    "org.scalaz" %% "scalaz-effect" % "7.4.0-M7"
  )
)

// Transport
lazy val sttp = (project in file("transport/sttp")).dependsOn(
  spi, testCore % Test, standard % Test
).settings(
  name := "json-rpc-sttp",
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client3" %% "core" % "3.3.9"
  )
)

// Server
lazy val undertow = (project in file("server/undertow")).dependsOn(
  core, testCore % Test, standard % Test
).settings(
  name := "json-rpc-undertow",
  libraryDependencies ++= Seq(
    "io.undertow" % "undertow-core" % "2.2.8.Final",
    "com.lihaoyi" %% "cask" % "0.7.11" % Test
  )
)
lazy val finagle = (project in file("server/finagle")).dependsOn(
  core, testCore % Test, standard % Test
).settings(
  name := "json-rpc-finagle",
  libraryDependencies ++= Seq(
    ("com.twitter" % "finagle-http" % "21.6.0").cross(CrossVersion.for3Use2_13)
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
  testBase, core, upickle, circe, argonaut
).settings(
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % "3.3.9"
  )
)


// Compile
ThisBuild / scalaVersion := "3.0.0"
ThisBuild / crossScalaVersions ++= Seq("2.13.6", "3.0.0")
ThisBuild / scalacOptions ++= Seq(
//  "-Djsonrpc.macro.debug=true",
  "-feature",
//  "-Xfatal-warnings",
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
//    "-new-syntax",
    "-language:strictEquality",
    "-language:adhocExtensions",
    "-indent",
    "-Xcheck-macros",
    "-Ysafe-init",
    "-pagewidth",
    "120"
  )
  case _ => Seq(
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
//scalastyleConfig := baseDirectory.value / "project" / "scalastyle.xml"
//scalastyleFailOnError := true
lazy val testScalastyle = taskKey[Unit]("testScalastyle")
testScalastyle := (Test / scalastyle).toTask("").value
//Test / test := ((Test / test) dependsOn testScalastyle).value


// Documentation
ThisBuild / autoAPIMappings := true
apiURL := Some(url("https://javadoc.io/doc/io.jsonrpc/jsonrpc-core_3/latest"))
ScalaUnidoc / unidoc / scalacOptions += "-Ymacro-expand:none"
//Compile / doc / scalacOptions ++= Seq("-groups", "-implicits")
//apiMappings += (
//  (unmanagedBase.value / "cats-effect.jar") -> 
//    url("https://example.org/api/")
//)


// Release
ThisBuild / releaseCrossBuild := true
ThisBuild / homepage := Some(url("https://github.com/jsonrpc/jsonrpc"))
ThisBuild / scmInfo := Some(ScmInfo(
  url("https://github.com/jsonrpc/jsonrpc"),
  "scm:git:git@github.com:jsonrpc/jsonrpc.git"
))
ThisBuild / releaseVcsSign := true
ThisBuild / releasePublishArtifactsAction := PgpKeys.publishSigned.value
ThisBuild / versionScheme := Some("semver-spec")
ThisBuild / publishTo := Some(MavenCache("local-maven", file("target/maven-relases")))


// Project
ThisBuild / version := "0.1.0"

lazy val root = project.in(file(".")).aggregate(
  core,
  codecUpickle,
  codecCirce,
  effectZio,
  effectMonix,
  effectCats,
  transportSttp,
  integration
).settings(
  name := "json-rpc",
  description := "JSON-RPC client & server"
).enablePlugins(ScalaUnidocPlugin)

// Dependencies

// Basic
lazy val util = project.settings(
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-api" % "1.7.30",
    "com.lihaoyi" %% "pprint" % "0.6.6",
  )
)
lazy val spi = project
lazy val test = project.dependsOn(
  spi
).settings(
  libraryDependencies ++= Seq(
    // Test
    "org.scalatest" %% "scalatest" % "3.2.9",
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0",
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )
)
lazy val core = project.dependsOn(
  util, spi, test % Test
)

// Codec
lazy val codecUpickle = (project in file("codec/upickle")).dependsOn(
  util, spi, test % Test
).settings(
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "upickle" % "1.3.15"
  )
)
lazy val codecCirce = (project in file("codec/circe")).dependsOn(
  util, spi, test % Test
).settings(
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-parser" % "0.14.1",
    "io.circe" %% "circe-generic" % "0.14.1"
  )
)

// Effect
lazy val effectZio = (project in file("effect/zio")).dependsOn(
  util, spi, test % Test
).settings(
  libraryDependencies ++= Seq(
    "dev.zio" %% "zio" % "1.0.8"
  )
)
lazy val effectMonix = (project in file("effect/monix")).dependsOn(
  util, spi, test % Test
).settings(
  libraryDependencies ++= Seq(
    "io.monix" %% "monix-eval" % "3.4.0"
  )
)
lazy val effectCats = (project in file("effect/cats")).dependsOn(
  util, spi, test % Test
).settings(
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "3.1.1"
  )
)

// Transport
lazy val transportSttp = (project in file("transport/sttp")).dependsOn(
  util, spi, test % Test
).settings(
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % "3.3.5"
  )
)

// Integration
lazy val integration = project.dependsOn(
  core, test % Test, codecUpickle % Test
)


// Compile
ThisBuild / scalaVersion := "3.0.0"
ThisBuild / scalacOptions ++= Seq(
  "-source",
  "future-migration",
  "-new-syntax",
  "-indent",
  "-feature",
  "-Xcheck-macros",
  "-Ysafe-init",
//  "-Xfatal-warnings",
  "-deprecation",
  "-unchecked",
  "-language:strictEquality",
  "-language:higherKinds",
  "-release",
  "8",
  "-encoding",
  "utf8",
  "-pagewidth",
  "120"
)

// Doc
ThisBuild / autoAPIMappings := true


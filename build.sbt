// Project
ThisBuild / organization := "io.json-rpc"
ThisBuild / version := "0.1.0"

lazy val root = project.in(file(".")).aggregate(
  core,

  upickle,
  circe,

  zio,
  monix,
  cats,
  scalaz,

  sttp,

  undertow
).settings(
  name := "json-rpc",
  description := "JSON-RPC client & server"
).enablePlugins(ScalaUnidocPlugin)


// Dependencies

// Core
lazy val spi = project.settings(
  name := "json-rpc-spi"
)
lazy val util = project.settings(
  name := "json-rpc-util",
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-api" % "1.7.30",
    "com.lihaoyi" %% "pprint" % "0.6.6",
  )
)
lazy val test = project.dependsOn(
  spi
).settings(
  name := "json-rpc-test",
  libraryDependencies ++= Seq(
    // Test
    "org.scalatest" %% "scalatest" % "3.2.9",
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0",
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )
)
lazy val core = project.dependsOn(
  util, spi, test % Test, upickle % Test
).settings(
  name := "json-rpc-core"
)

// Codec
lazy val upickle = (project in file("codec/upickle")).dependsOn(
  util, spi, test % Test
).settings(
  name := "json-rpc-upickle",
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "upickle" % "1.3.15"
  )
)
lazy val circe = (project in file("codec/circe")).dependsOn(
  util, spi, test % Test
).settings(
  name := "json-rpc-circe",
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-parser" % "0.14.1",
    "io.circe" %% "circe-generic" % "0.14.1"
  )
)

// Effect
lazy val zio = (project in file("backend/zio")).dependsOn(
  util, spi, test % Test
).settings(
  name := "json-rpc-zio",
  libraryDependencies ++= Seq(
    "dev.zio" %% "zio" % "1.0.8"
  )
)
lazy val monix = (project in file("backend/monix")).dependsOn(
  util, spi, test % Test
).settings(
  name := "json-rpc-monix",
  libraryDependencies ++= Seq(
    "io.monix" %% "monix-eval" % "3.4.0"
  )
)
lazy val cats = (project in file("backend/cats")).dependsOn(
  util, spi, test % Test
).settings(
  name := "json-rpc-cats",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "3.1.1"
  )
)
lazy val scalaz = (project in file("backend/scalaz")).dependsOn(
  util, spi, test % Test
).settings(
  name := "json-rpc-scalaz",
  libraryDependencies ++= Seq(
    "org.scalaz" %% "scalaz-effect" % "7.4.0-M7"
  )
)

// Transport
lazy val sttp = (project in file("transport/sttp")).dependsOn(
  util, spi, test % Test
).settings(
  name := "json-rpc-sttp",
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % "3.3.5"
  )
)

// Server
lazy val undertow = (project in file("server/undertow")).dependsOn(
  core, test % Test, upickle % Test
).settings(
  name := "json-rpc-undertow",
  libraryDependencies ++= Seq(
    "io.undertow" % "undertow-core" % "2.2.8.Final",
    "com.lihaoyi" %% "cask" % "0.7.11" % Test
  )
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


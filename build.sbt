// Project
ThisBuild / version := "0.1.0"

lazy val root = project.in(file(".")).aggregate(
  core
).dependsOn(
  core,
  test % Test
).settings(
  name := "json-rpc",
  description := "JSON-RPC client & server",
  libraryDependencies ++= Seq(
    // Format
    "com.lihaoyi" %% "upickle" % "1.3.15",
    "io.circe" %% "circe-parser" % "0.14.1",
    "io.circe" %% "circe-generic" % "0.14.1",
    ("com.fasterxml.jackson.module" % "jackson-module-scala" % "2.12.3").cross(CrossVersion.for3Use2_13),

    // Effect
    "dev.zio" %% "zio" % "1.0.8",
    "io.monix" %% "monix-eval" % "3.4.0",
//    "org.typelevel" %% "cats-effect" % "3.1.1",

    // Transport
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % "3.3.5",

  )
)

// Dependencies
lazy val util = project

lazy val spi = project

lazy val test = project.dependsOn(
  util,
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
  util,
  spi,
  test % Test
).settings(
  libraryDependencies ++= Seq(
    // Utilities
    "org.slf4j" % "slf4j-api" % "1.7.30",
    "com.lihaoyi" %% "pprint" % "0.6.6",
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

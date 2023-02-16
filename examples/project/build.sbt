scalaVersion := "3.2.2"
name := "automorph-example"
organization := "test"
version := "0.0.1"

Test / parallelExecution := false

val automorphVersion = "0.0.1-SNAPSHOT"
val sttpVersion = "3.8.11"
libraryDependencies ++= Seq(
  // Default
  "org.automorph" %% "automorph-default" % automorphVersion,
  "ch.qos.logback" % "logback-classic" % "1.4.5",

  // Plugins
  "org.automorph" %% "automorph-upickle" % automorphVersion % Test,
  "org.automorph" %% "automorph-zio" % automorphVersion % Test,
  "org.automorph" %% "automorph-sttp" % automorphVersion % Test,
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % sttpVersion % Test,
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpVersion % Test,

  // Test
  "org.scalatest" %% "scalatest" % "3.2.15" % Test
)

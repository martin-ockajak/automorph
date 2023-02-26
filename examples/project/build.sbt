scalaVersion := "3.2.2"
name := "automorph"
organization := "example"
version := "0.0.0"

Test / parallelExecution := false

val automorphVersion = "0.0.0"
val sttpVersion = "3.8.11"
libraryDependencies ++= Seq(
  // Default
  "org.automorph" %% "automorph-default" % automorphVersion,
  "ch.qos.logback" % "logback-classic" % "1.4.5",

  // Plugins
  "org.automorph" %% "automorph-upickle" % automorphVersion,
  "org.automorph" %% "automorph-zio" % automorphVersion,
  "org.automorph" %% "automorph-sttp" % automorphVersion,
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % sttpVersion,
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpVersion,
  "io.arivera.oss" % "embedded-rabbitmq" % "1.5.0",

  // Test
  "org.scalatest" %% "scalatest" % "3.2.15" % Test
)

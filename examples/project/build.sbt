scalaVersion := "3.0.0"
name := "automorph-examples"
organization := "test"
version := "0.0.1"

Test / parallelExecution := false

val automorphVersion = "0.0.1-SNAPSHOT"
libraryDependencies ++= Seq(
  // Default
  "org.automorph" %% "automorph-default" % automorphVersion,
  "ch.qos.logback" % "logback-classic" % "1.3.0-alpha10",

  // Plugins
  "org.automorph" %% "automorph-zio" % automorphVersion % Test,
  "org.automorph" %% "automorph-upickle" % automorphVersion % Test,

  // Test
  "org.scalatest" %% "scalatest" % "3.2.10" % Test
)


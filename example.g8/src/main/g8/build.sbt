scalaVersion := "3.0.0"
name := "automorph-example"
organization := "test"
version := "0.0.1"

val automorphVersion = "0.0.1-SNAPSHOT"
libraryDependencies ++= Seq(
  // Default
  "org.automorph" %% "automorph-default" % automorphVersion,
  "ch.qos.logback" % "logback-classic" % "1.3.0-alpha10",

  // Test
  "org.scalatest" %% "scalatest" % "3.2.10" % Test
)


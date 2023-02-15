scalaVersion := "3.2.2"
name := "automorph-example"
organization := "test"
version := "0.0.1"

val automorphVersion = "0.0.1-SNAPSHOT"
libraryDependencies ++= Seq(
  // Default
  "org.automorph" %% "automorph-default" % automorphVersion,
  "ch.qos.logback" % "logback-classic" % "1.4.5",

  // Test
  "org.scalatest" %% "scalatest" % "3.2.15" % Test
)

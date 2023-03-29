scalaVersion := "3.2.2"
name := "automorph"
organization := "example"
version := "0.0.0"

val automorphVersion = "0.0.0"
libraryDependencies ++= Seq(
  // Default
  "org.automorph" %% "automorph-default" % automorphVersion,
  "ch.qos.logback" % "logback-classic" % "1.4.6",

  // Test
  "org.scalatest" %% "scalatest" % "3.2.15" % Test
)

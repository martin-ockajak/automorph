lazy val root = project.in(file(".")).settings(
  name := "json-rpc",
  description := "JSON-RPC client & server",
  version := "0.1.0",
  scalaVersion := "3.0.0",
  libraryDependencies ++= Seq(
    // Format
    "com.lihaoyi" %% "upickle" % "1.3.15",
    "io.circe" %% "circe-parser" % "0.14.0-M7",
    "com.fasterxml.jackson.module" % "jackson-module-scala_2.13" % "2.12.3",

    // Effect
    "dev.zio" %% "zio" % "1.0.8",
    "io.monix" %% "monix-eval" % "3.4.0",
//    "org.typelevel" %% "cats-effect" % "3.1.1",

    // Test
    "org.scalatest" %% "scalatest" % "3.2.9" % Test,
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % Test
  )
)

scalacOptions ++= Seq(
  "-source",
  "future-migration",
  "-new-syntax",
  "-indent",
  "-feature",
  "-Xcheck-macros",
  "-Ysafe-init",
  "-Xfatal-warnings",
  "-deprecation",
  "-unchecked",
  "-language:strictEquality",
  "-language:higherKinds",
  "-release",
  "9",
  "-encoding",
  "utf8",
  "-pagewidth",
  "120"
)

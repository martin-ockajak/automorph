lazy val root = project.in(file(".")).settings(
  name := "json-rpc",
  description := "JSON-RPC client & server",
  version := "0.1.0",
  scalaVersion := "3.0.0-RC3",
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.8" % Test,
    "com.lihaoyi" %% "upickle" % "1.3.12",
    "com.fasterxml.jackson.module" % "jackson-module-scala_2.13" % "2.12.3",
    "dev.zio" %% "zio" % "1.0.7"
  )
)

scalacOptions ++= Seq(
  "-source", "future-migration",
  "-new-syntax",
  "-indent",
  "-feature",
  "-Yexplicit-nulls",
  "-Ysafe-init",
//  "-Werror",
//  "-Wextra-implicit",
//  "-Wnumeric-widen",
//  "-Xlint",
//  "-Vimplicits",
//  "-Vfree-terms",
//  "-Vfree-types",
  "-deprecation",
  "-unchecked",
  "-language:strictEquality",
  "-language:higherKinds",
  "-release", "9",
//  "-target:12",
//  "-opt:l:method,inline",
//  "-opt-inline-from:<sources>",
//  "-Ybackend-parallelism", "4",
  "-encoding", "utf8",
  "-pagewidth", "160",
)

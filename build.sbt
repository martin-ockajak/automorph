lazy val root = project.in(file(".")).settings(
  name := "json-rpc",
  description := "JSON-RPC client & server",
  version := "0.1.0",
  scalaVersion := "3.0.1-RC1-bin-20210515-83e17f1-NIGHTLY",
  libraryDependencies ++= Seq(
    // JSON
    "com.lihaoyi" %% "upickle" % "1.3.13",
    "com.fasterxml.jackson.module" % "jackson-module-scala_2.13" % "2.12.3",

    // Effect
//    "dev.zio" % "zio" % "1.0.7",
    "io.monix" %% "monix-eval" % "3.4.0",
//    "org.typelevel" %% "cats-effect" % "3.1.1",

    // Test
    "org.scalatest" %% "scalatest" % "3.2.9" % Test,
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % Test
  )
)

scalacOptions ++= Seq(
  "-source", "future-migration",
  "-new-syntax",
  "-indent",
  "-feature",
  "-Xcheck-macros",
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

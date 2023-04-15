// Project
val projectRoot = "org"
val projectName = "automorph"
val projectDescription = "RPC client and server for Scala"
val siteUrl = s"https://$projectName.$projectRoot"
val apiUrl = s"$siteUrl/api"
ThisBuild / homepage := Some(url(siteUrl))
ThisBuild / licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / description := projectDescription
ThisBuild / organization := s"$projectRoot.$projectName"
ThisBuild / organizationName := projectName
ThisBuild / organizationHomepage := Some(url(siteUrl))
ThisBuild / developers := List(Developer(
  id = "m",
  name = "Martin Ockajak",
  email = "automorph.org@proton.me",
  url = url(s"https://github.com/martin-ockajak")
))
Global / onChangedBuildSource := ReloadOnSourceChanges


// Repository
val repositoryPath = s"martin-ockajak/$projectName"
val repositoryUrl = s"https://github.com/$repositoryPath"
val repositoryShell = s"git@github.com:$repositoryPath.git"
ThisBuild / scmInfo := Some(ScmInfo(url(repositoryUrl), s"scm:$repositoryShell"))
apiURL := Some(url(apiUrl))
onLoadMessage := ""


// Structure
lazy val root = project.in(file(".")).settings(name := projectName, publish / skip := true).aggregate(
  // Core
  meta,
  core,

  // Message codec
  circe,
  jackson,
  upickle,
  argonaut,

  // Effect system
  standard,
  zio,
  monix,
  catsEffect,
  scalazEffect,

  // Client transport
  sttp,
  rabbitmq,

  // Server transport
  tapir,
  undertow,
  vertx,
  jetty,
  akkaHttp,

  // Endpoint transport
  finagle,

  // Misc
  default,
  examples
)


// Dependencies
def source(project: Project, path: String, dependsOn: ClasspathDep[ProjectReference]*): Project = {
  val subProject = project.in(file(path)).dependsOn(dependsOn: _*).settings(
    Compile / doc / scalacOptions := (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => docScalac3Options
      case _ => docScalac2Options
    }),
  )
  path.split('/') match {
    case Array(directory, _ @_*) if Seq("examples", "test").contains(directory) => subProject
    case Array(directory) => subProject.settings(
        name := s"$projectName-$directory"
      )
    case Array(_, directories @ _*) => subProject.settings(
        name := s"$projectName-${directories.mkString("-")}"
      )
  }
}

// Core
val slf4jVersion = "1.7.36"
lazy val meta = source(project, "meta").settings(
  libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) => Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value)
    case _ => Seq.empty
  }) ++ Seq(
    "org.slf4j" % "slf4j-api" % slf4jVersion
  )
)
lazy val core = source(project, "core", meta, testPlugin % Test)

// Effect system
lazy val standard = source(project, "system/standard", core, testPlugin % Test, testRpc % Test)
lazy val zio = source(project, "system/zio", core, testTransport % Test).settings(
  libraryDependencies += "dev.zio" %% "zio" % "2.0.10"
)
lazy val monix = source(project, "system/monix", core, testTransport % Test).settings(
  libraryDependencies += "io.monix" %% "monix-eval" % "3.4.1"
)
lazy val catsEffect = source(project, "system/cats-effect", core, testTransport % Test).settings(
  libraryDependencies += "org.typelevel" %% "cats-effect" % "3.4.8"
)
lazy val scalazEffect = source(project, "system/scalaz-effect", core, testTransport % Test).settings(
  libraryDependencies += "org.scalaz" %% "scalaz-effect" % "7.4.0-M13"
)

// Message codec
val circeVersion = "0.14.5"
lazy val circe = source(project, s"codec/circe", core, testPlugin % Test).settings(
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion
  )
)
val jacksonVersion = "2.14.2"
lazy val jackson = source(project, "codec/jackson", core, testPlugin % Test).settings(
  libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion
)
lazy val upickle = source(project, "codec/upickle", core, testPlugin % Test).settings(
  libraryDependencies += "com.lihaoyi" %% "upickle" % "2.0.0"
)
lazy val argonaut = source(project, "codec/argonaut", core, testPlugin % Test).settings(
  libraryDependencies += "io.argonaut" %% "argonaut" % "6.3.8"
)

// Client transport
val sttpVersion = "3.8.13"
val sttpHttpClientVersion = "3.5.2"
lazy val sttp = source(project, "transport/sttp", core, testTransport % Test).settings(
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % sttpVersion % Test,
    "com.softwaremill.sttp.client3" %% "httpclient-backend" % sttpHttpClientVersion % Test
  )
)
val embeddedRabbitMqVersion = "1.5.0"
lazy val rabbitmq = source(project, "transport/rabbitmq", core, testTransport % Test).settings(
  Test / fork := true,
  Test / testForkedParallel := true,
  libraryDependencies ++= Seq(
    "com.rabbitmq" % "amqp-client" % "5.17.0",
    "io.arivera.oss" % "embedded-rabbitmq" % embeddedRabbitMqVersion % Test
  )
)

// Server transport
val tapirVersion = "1.2.11"
lazy val tapir = source(project, "transport/tapir", core, testTransport % Test).settings(
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-server" % tapirVersion,
//    ("com.softwaremill.sttp.tapir" %% "tapir-akka-http-server" % tapirVersion % Test).cross(CrossVersion.for3Use2_13)
//      .exclude("com.softwaremill.sttp.tapir", "tapir-server_2.13")
//      .exclude("com.softwaremill.sttp.shared", "core_2.13"),
//    ("com.typesafe.akka" %% "akka-actor-typed" % akkaVersion % Test).cross(CrossVersion.for3Use2_13),
//    ("com.typesafe.akka" %% "akka-stream" % akkaVersion % Test).cross(CrossVersion.for3Use2_13),
    "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion % Test,
    "com.softwaremill.sttp.tapir" %% "tapir-netty-server" % tapirVersion % Test,
    ("com.softwaremill.sttp.tapir" %% "tapir-finatra-server" % tapirVersion % Test).cross(CrossVersion.for3Use2_13)
      .exclude("com.softwaremill.sttp.tapir", "tapir-server_2.13")
      .exclude("org.scala-lang.modules", "scala-collection-compat_2.13")
      .exclude("com.fasterxml.jackson.module", "jackson-module-scala_2.13")
//    "com.softwaremill.sttp.tapir" %% "tapir-vertx-server" % tapirVersion % Test
  )
)
lazy val undertow = source(project, "transport/undertow", core, testTransport % Test).settings(
  libraryDependencies += "io.undertow" % "undertow-core" % "2.3.4.Final"
)
lazy val vertx = source(project, "transport/vertx", core, testTransport % Test).settings(
  libraryDependencies += "io.vertx" % "vertx-core" % "4.4.0"
)
val jettyVersion = "11.0.14"
lazy val jetty = source(project, "transport/jetty", core, testTransport % Test).settings(
  libraryDependencies ++= Seq(
    "org.eclipse.jetty.websocket" % "websocket-jetty-client" % jettyVersion,
    "org.eclipse.jetty" % "jetty-servlet" % jettyVersion,
    "org.eclipse.jetty.websocket" % "websocket-jetty-server" % jettyVersion
  )
)
val akkaVersion = "2.8.0"
lazy val akkaHttp = source(project, "transport/akka-http", core, testTransport % Test).settings(
  Test / fork := true,
  Test / testForkedParallel := true,
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http" % "10.5.0",
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion % Test
  )
)

// Endpoint transport
lazy val finagle = source(project, "transport/finagle", core, testTransport % Test).settings(
  libraryDependencies ++= Seq(
    ("com.twitter" % "finagle-http" % "22.12.0")
      .exclude("org.scala-lang.modules", "scala-collection-compat_2.13")
      .exclude("com.fasterxml.jackson.module", "jackson-module-scala_2.13")
      .cross(CrossVersion.for3Use2_13),
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion
  )
)

// Miscellaneous
lazy val default = project.dependsOn(standard, circe, undertow, testTransport % Test).settings(
  name := s"$projectName-default",
  libraryDependencies += "com.softwaremill.sttp.client3" %% "httpclient-backend" % sttpHttpClientVersion
)
lazy val examples = source(
  project, "examples", default, upickle, zio, sttp, rabbitmq, testPlugin % Test
).settings(
  publish / skip := true,
  Test / fork := true,
  Test / testForkedParallel := true,
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % sttpVersion,
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpVersion,
    "io.arivera.oss" % "embedded-rabbitmq" % embeddedRabbitMqVersion
  ),
  Compile / scalaSource :=  baseDirectory.value / "project/src/main/scala",
  Test / scalaSource := baseDirectory.value / "project/src/test/scala"
)


// Test
ThisBuild / Test / testOptions += Tests.Argument("-oDF")
val scribeVersion = "3.11.1"
lazy val testBase = source(project, "test/base").settings(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.15",
    "org.scalatestplus" %% "scalacheck-1-17" % "3.2.15.0",
    "com.outr" %% "scribe-file" % scribeVersion,
    "com.outr" %% "scribe-slf4j" % scribeVersion,
    "org.slf4j" % "jul-to-slf4j" % slf4jVersion,
    "com.lihaoyi" %% "pprint" % "0.8.1"
  )
)
lazy val testRpc = source(project, "test/rpc", testBase, core, circe, jackson, upickle, argonaut)
lazy val testPlugin = source(project, "test/plugin", testBase, meta).settings(
  libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion
)
lazy val testTransport = source(
  project, "test/transport", testRpc, standard,
)


// Compile
ThisBuild / scalaVersion := "3.2.2"
ThisBuild / crossScalaVersions += "2.13.10"
ThisBuild / javacOptions ++= Seq("-source", "11", "-target", "11")
val commonScalacOptions = Seq(
  "-language:higherKinds",
  "-feature",
  "-deprecation",
  "-unchecked",
  "-release",
  "11",
  "-encoding",
  "utf8"
)
val compileScalac3Options = commonScalacOptions ++ Seq(
  "-source",
  "3.2",
//  "-Wunused",
  "-language:adhocExtensions",
  "-pagewidth",
  "120"
)
val compileScalac2Options = commonScalacOptions ++ Seq(
  "-language:existentials",
  "-Xsource:3",
  "-Xlint:_,-byname-implicit",
  "-Wconf:site=[^.]+\\.codec\\.json\\..*:silent,cat=other-non-cooperative-equals:silent",
  "-Wextra-implicit",
  "-Wnumeric-widen",
  "-Wvalue-discard",
  "-Wunused:imports,patvars,privates,locals,params",
  "-Vfree-terms",
  "-Vimplicits",
  "-Ybackend-parallelism",
  "4"
)
val docScalac3Options = compileScalac3Options ++ Seq(
  s"-source-links:src=github://$repositoryPath/master",
  "-skip-by-id:automorph.client,automorph.handler,automorph.spi.codec"
)
val docScalac2Options = compileScalac2Options ++ Seq(
  "-skip-packages",
  "automorph.client:automorph.handler:automorph.spi.codec"
)
ThisBuild / scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((3, _)) => compileScalac3Options ++ Seq(
    "-indent",
    "-Xcheck-macros",
    "-Ysafe-init"
  )
  case _ => compileScalac2Options
})


// Analyze
scalastyleConfig := baseDirectory.value / "project/scalastyle-config.sbt.xml"
Compile / scalastyleSources ++= (Compile / unmanagedSourceDirectories).value
scalastyleFailOnError := true
lazy val testScalastyle = taskKey[Unit]("testScalastyle")
testScalastyle := (Test / scalastyle).toTask("").value
Test / test := (Test / test).dependsOn(testScalastyle).value


// Documentation
def flattenTasks[A](tasks: Seq[Def.Initialize[Task[A]]]): Def.Initialize[Task[Seq[A]]] =
  tasks match {
    case Seq() => Def.task(Seq())
    case Seq(head, tail @ _*) => Def.taskDyn(flattenTasks(tail).map(_.+:(head.value)))
  }

lazy val allSources = Def.taskDyn(flattenTasks(root.uses.map(_ / Compile / doc / sources)))
lazy val allTastyFiles = Def.taskDyn(flattenTasks(root.uses.map(_ / Compile / doc / tastyFiles)))
lazy val allDependencyClasspath = Def.taskDyn(flattenTasks(root.uses.map(_ / Compile / doc / dependencyClasspath)))
lazy val docs = project.in(file("site")).settings(
  name := projectName,
  mdocVariables := Map(
    "PROJECT_VERSION" -> version.value,
    "SCALADOC_VERSION" -> scalaVersion.value,
    "REPOSITORY_URL" -> repositoryUrl
  ),
  mdocOut := baseDirectory.value / "docs",
  mdocExtraArguments := Seq("--no-link-hygiene"),
  mdoc / fileInputs ++= Seq(
    (LocalRootProject / baseDirectory).value.toGlob / "docs" / ** / "*.md",
    (LocalRootProject / baseDirectory).value.toGlob / "docs" / ** / "*.jpg"
  ),
  Compile / doc / scalacOptions := docScalac3Options,
  Compile / doc / sources ++= allSources.value.flatten.filter(_.getName != "MonixSystem.scala"),
  Compile / doc / tastyFiles ++= allTastyFiles.value.flatten.filter(_.getName != "MonixSystem.tasty"),
  Compile / doc / dependencyClasspath ++=
    allDependencyClasspath.value.flatten.filter(_.data.getName != "cats-effect_3-2.5.4.jar")
).enablePlugins(MdocPlugin)


// Site
def relativizeScaladocLinks(content: String, path: String): String = {
  import java.io.File
  val searchData = path.endsWith(s"${File.separator}searchData.js")
  if (searchData || path.endsWith(".html")) {
    val apiLinkPrefix = """"https:\/\/javadoc\.io\/page\/org\.automorph\/[^\/]+\/[^\/]+\/"""
    val patterns = path.split(File.separator).toSeq.init.foldLeft(Seq("")) { case (paths, packageName) =>
      paths :+ s"${paths.last}$packageName/"
    }.reverse
    val replacements = if (searchData) Seq("", s"$apiUrl/") else Range(0, patterns.size).map("../" * _)
    val titledContent = content.replaceAll(">root<", s">${projectName.capitalize}<")
    patterns.zip(replacements).foldLeft(titledContent) { case (text, (pattern, replacement)) =>
      text.replaceAll(s"$apiLinkPrefix$pattern", s""""$replacement""")
    }
  } else {
    content
  }
}

// Generate
val site = taskKey[Unit]("Generates project website.")
site := {
  import scala.sys.process.Process
  (docs / Compile / doc).value
  (docs / mdoc).toTask("").value
  Process(Seq("yarn", "install"), (docs / baseDirectory).value).!
  Process(Seq("yarn", "build"), (docs / baseDirectory).value, "SITE_DOCS" -> "docs").!
  val apiDirectory = (docs / baseDirectory).value / "build/api"
  val systemDirectory = s"$projectName/system"
  Path.allSubpaths((docs / Compile / doc / target).value).filter(_._1.isFile).foreach { case (file, path) =>
    IO.write(apiDirectory / path, relativizeScaladocLinks(IO.read(file), path))
  }
  Path.allSubpaths(
    (monix / Compile / doc / target).value / systemDirectory
  ).filter(_._1.isFile).foreach { case (file, path) =>
    IO.write(apiDirectory / systemDirectory / path, relativizeScaladocLinks(IO.read(file), path))
  }
  val examplesDirectory = (docs / baseDirectory).value / "build/examples/project"
  IO.copyDirectory((examples / baseDirectory).value / "project", examplesDirectory, overwrite = true)
}

// Start
val startSite = taskKey[Unit]("Continuously generates project website.")
startSite := {
  import scala.sys.process.Process
  Process(Seq("yarn", "install"), (docs / baseDirectory).value).!
  Process(Seq("yarn", "start"), (docs / baseDirectory).value, "SITE_DOCS" -> "docs").!
}
startSite := startSite.dependsOn(site).value

// Serve
val serveSite = taskKey[Unit]("Serve generated project website.")
serveSite := {
  import scala.sys.process.Process
  Process(Seq("yarn", "install"), (docs / baseDirectory).value).!
  Process(Seq("yarn", "serve"), (docs / baseDirectory).value, "SITE_DOCS" -> "docs").!
}
serveSite := serveSite.dependsOn(site).value

cleanFiles ++= Seq(
  (docs / baseDirectory).value / "build",
  (docs / baseDirectory).value / "docs",
  (docs / baseDirectory).value / "static/examples"
)


// Release
val repositoryCredentialsPath = Path.userHome / ".sbt/sonatype_credentials"
ThisBuild / publishTo := {
  Some(if (isSnapshot.value) {
    "snapshots".at("https://s01.oss.sonatype.org/content/repositories/snapshots")
  } else {
    "releases".at("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
  })
}
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishMavenStyle := true
ThisBuild / releaseCrossBuild := true
ThisBuild / releaseVcsSign := true
ThisBuild / releasePublishArtifactsAction := PgpKeys.publishSigned.value
ThisBuild / versionScheme := Some("early-semver")
credentials ++= Seq(
  Credentials("GnuPG Key ID", "gpg", "9E5F3CBE696BE49391A5131EFEAB85EB98F65E63", "")
) ++ (if (repositoryCredentialsPath.isFile) {
  Seq(Credentials(repositoryCredentialsPath))
} else {
  Seq.empty
})

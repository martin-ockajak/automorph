![automorph](https://github.com/martin-ockajak/automorph/raw/main/project/images/logo.jpg)

**Automorph** is an [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) client and server library for [Scala](https://www.scala-lang.org/) providing an effortless
way to invoke and expose remote APIs while supporting multiple RPC protocols such as [JSON-RPC](https://www.jsonrpc.org/specification) and highly standardized form of [REST](https://en.wikipedia.org/wiki/Representational_state_transfer).

[![Build](https://github.com/martin-ockajak/automorph/workflows/Build/badge.svg)](https://github.com/martin-ockajak/automorph/actions/workflows/tests.yml)
[![Releases](https://img.shields.io/maven-central/v/io.automorph/automorph-core_2.13.svg)](https://mvnrepository.com/artifact/io.automorph)
[![Scaladoc](https://javadoc-badge.appspot.com/io.automorph/automorph-core_2.13.svg?label=scaladoc)](https://javadoc.io/doc/io.automorph/automorph-core_2.13/latest/automorph/)

- [Overview](#overview)
  - [Goals](#goals)
  - [Features](#features)
  - [Inspiration](#inspiration)
- [Quickstart](#quickstart)
  - [Scaladoc](#scaladoc)
  - [Build](#build)
  - [API](#api)
  - [Server](#server)
  - [Client](#client)
  - [Dynamic Client](#dynamic-client)
- [Integration](#integration)
  - [Effect system](#effect-system)
  - [Message format](#message-format)
  - [Message transport](#message-transport)
    - [Client transport](#client-transport)
    - [Endpoint transport](#endpoint-transport)
    - [Server transport](#server-transport)
- [Architecture](#architecture)
- [Examples](#examples)
  - [Synchronous](#synchronous)
  - [Asynchronous](#asynchronous)
  - [Request context](#request-context)
  - [Method alias](#method-alias)
  - [Choose effect system](#choose-effect-system)
  - [Choose message transport](#choose-message-transport)
  - [Choose message format](#choose-message-format)

# Overview

## Goals

* Provide a definitive RPC solution for Scala ecosystem
* Require minimal effort and disruption of existing codebase
* Encourage use of standard interoperability protocols

## Features

* **Powerful** - generate client and server bindings directly from public methods of your API classes
* **Modular** - combine integration plugins to match your chosen effect type, message format and message transport protocol
* **Clean** - access request and response metadata without polluting your API abstractions
* **Safe** - type checks bound API classes during compilation
* **Fast** - generates optimized API binding code during compilation
* **Flexible** - customize remote API method names and error mapping
* **Extensible** - automatically derive or implement serialization of arbitrary data types
* **Compatible** - available for [Scala 2.12](https://www.scala-lang.org/news/2.12.0/), [Scala 2.13](https://www.scala-lang.org/news/2.13.0)
  and [Scala 3](https://dotty.epfl.ch/)
* **Boilerplate free** - even advanced use-cases require only a few lines of code
* **Dependency free** - core logic depends on [SLF4J API](http://www.slf4j.org/) only

## Inspiration

* [Scala JSON-RPC](https://github.com/shogowada/scala-json-rpc)
* [Autowire](https://github.com/lihaoyi/autowire)
* [STTP](https://github.com/softwaremill/sttp)

# [Quickstart](/examples/src/test/scala/examples/Asynchronous.scala)

Exposing and invoking a JSON-RPC API using HTTP as transport protocol.

## [Scaladoc](https://www.javadoc.io/doc/io.automorph/automorph-core_2.13/latest/)

## Build

Add the following to your `build.sbt` file:

```scala
libraryDependencies += "io.automorph" %% "automorph-default" % "1.0.0"
```

### API

Take an existing asynchronous API:

```scala
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Define an API type and create API instance
class Api {
  def hello(some: String, n: Int): Future[String] = Future.successful(s"Hello $some $n!")
}
val api = new Api()

```

Expose the API via JSON-RPC over HTTP(S).

### Server

```scala
// Create and start RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer.async(_.bind(api), 80, "/api")

// Stop the server
server.close()
```

### Client

Invoke the API via JSON-RPC over HTTP(S).

```scala
// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val client = automorph.DefaultHttpClient.async("http://localhost/api", "POST")

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // : Future[String]
```

### Dynamic Client

Invoke the API dynamically without definition via JSON-RPC over HTTP(S).

```scala
// Call a remote API method dynamically passing the arguments by name
val hello = client.method("hello")
hello.args("some" -> "world", "n" -> 1).call[String] // Future[String]

// Call a remote API method dynamically passing the arguments by position
hello.positional.args("world", 1).call[String] // Future[String]

// Notify a remote API method dynamically passing the arguments by name
hello.args("some" -> "world", "n" -> 1).tell // Future[Unit]

// Notify a remote API method dynamically passing the arguments by position
hello.positional.args("world", 1).tell // Future[Unit]
```

# Integration

*Automorph* supports integration with various libraries via plugins published in different artifacts.

## [Effect system](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/EffectSystem.html)

Computational effect system plugins.

The underlying runtime must support monadic composition of effectful values.

| Class | Artifact | Library | Effect Type |
| ---- | --- | --- | --- |
| [FutureSystem](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/system/FutureSystem.html) (Default) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard](https://docs.scala-lang.org/overviews/core/futures.html) | [Future](https://www.scala-lang.org/api/current/scala/concurrent/Future.html) |
| [TrySystem](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/system/TrySystem.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard](https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html) | [Try](https://www.scala-lang.org/api/2.13.6/scala/util/Try.html) |
| [IdentitySystem](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/system/IdentitySystem.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard](https://www.scala-lang.org/) | [Identity](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/system/IdentitySystem$$Identity.html) |
| [ZioSystem](https://www.javadoc.io/doc/io.automorph/automorph-zio_2.13/latest/automorph/system/ZioSystem.html) | [automorph-zio](https://mvnrepository.com/artifact/io.automorph/automorph-zio) | [ZIO](https://zio.dev/) | [RIO](https://javadoc.io/doc/dev.zio/zio_2.13/latest/zio/RIO$.html) |
| [MonixSystem](https://www.javadoc.io/doc/io.automorph/automorph-monix_2.13/latest/automorph/system/MonixSystem.html) | [automorph-monix](https://mvnrepository.com/artifact/io.automorph/automorph-monix) | [Monix](https://monix.io/) | [Task](https://monix.io/api/current/monix/eval/Task.html) |
| [CatsEffectSystem](https://www.javadoc.io/doc/io.automorph/automorph-cats-effect_2.13/latest/automorph/system/CatsEffectSystem.html) | [automorph-cats-effect](https://mvnrepository.com/artifact/io.automorph/automorph-cats-effect) | [Cats Effect](https://typelevel.org/cats-effect/) | [IO](https://www.javadoc.io/doc/org.typelevel/cats-effect_3/latest/cats/effect/IO.html) |
| [ScalazSystem](https://www.javadoc.io/doc/io.automorph/automorph-scalaz_2.13/latest/automorph/system/ScalazSystem.html) | [automorph-scalaz](https://mvnrepository.com/artifact/io.automorph/automorph-scalaz) | [Scalaz](https://github.com/scalaz) | [IO](https://www.javadoc.io/doc/org.scalaz/scalaz_2.13/latest/scalaz/effect/IO.html) |

## [Message format](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/MessageFormat.html)

Structured message format serialization/deserialization plugins.

The underlying format must support storing arbitrarily nested structures of basic data types.

| Class | Artifact | Library | Node Type | Format |
| ---- | --- | --- | --- | --- |
| [UpickleJsonFormat](https://www.javadoc.io/doc/io.automorph/automorph-upickle_2.13/latest/automorph/format/json/UpickleJsonFormat.html) (Default) | [automorph-upickle](https://mvnrepository.com/artifact/io.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Value](http://com-lihaoyi.github.io/upickle/#uJson) | [JSON](https://www.json.org/) |
| [UpickleMessagePackFormat](https://www.javadoc.io/doc/io.automorph/automorph-upickle_2.13/latest/automorph/format/messagepack/UpickleMessagePackFormat.html) | [automorph-upickle](https://mvnrepository.com/artifact/io.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Msg](http://com-lihaoyi.github.io/upickle/#uPack) | [MessagePack](https://msgpack.org/) |
| [CirceJsonFormat](https://www.javadoc.io/doc/io.automorph/automorph-circe_2.13/latest/automorph/format/json/CirceJsonFormat.html) | [automorph-circe](https://mvnrepository.com/artifact/io.automorph/automorph-circe) | [Circe](https://circe.github.io/circe) |[Json](https://circe.github.io/circe/api/io/circe/Json.html) | [JSON](https://www.json.org/) |
| [ArgonautJsonFormat](https://www.javadoc.io/doc/io.automorph/automorph-argonaut_2.13/latest/automorph/format/json/ArgonautJsonFormat.html) | [automorph-argonaut](https://mvnrepository.com/artifact/io.automorph/automorph-argonaut) | [Argonaut](http://argonaut.io/doc/) |[Json](http://argonaut.io/scaladocs/#argonaut.Json) | [JSON](https://www.json.org/) |

## [Mesage transport](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/MessageTransport.html)

Message transport protocol plugins.

The underlying transport protocol must support implementation of request-response pattern.

### [Client transport](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/ClientMessageTransport.html)

Client message transport protocol plugins.

Used by the RPC client to send requests and receive responses to and from a remote endpoint.

| Class | Artifact | Library | Protocol |
| ---- | --- | --- | --- |
| [SttpClient](https://www.javadoc.io/doc/io.automorph/automorph-sttp_2.13/latest/automorph/transport/http/client/SttpClient.html) (Default) | [automorph-sttp](https://mvnrepository.com/artifact/io.automorph/automorph-sttp) | [STTP](https://sttp.softwaremill.com/en/latest/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [UrlConnectionClient](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/transport/http/client/UrlConnectionClient.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard](https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [RabbitMqClient](https://www.javadoc.io/doc/io.automorph/automorph-rabbitmq_2.13/latest/automorph/transport/amqp/client/RabbitMqClient.html) | [automorph-rabbitmq](https://mvnrepository.com/artifact/io.automorph/automorph-rabbitmq) | [RabbitMq](https://www.rabbitmq.com/java-client.html) | [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol) |

### [Server transport](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/ServerMessageTransport.html)

Server message transport protocol plugins.

Used to actively receive and reply to requests using specific message transport protocol
while invoking RPC request handler to process them.

| Class | Artifact | Library | Protocol |
| ---- | --- | --- | --- |
| [UndertowServer](https://www.javadoc.io/doc/io.automorph/automorph-undertow_2.13/latest/automorph/transport/http/server/UndertowServer.html) (Default) | [automorph-undertow](https://mvnrepository.com/artifact/io.automorph/automorph-undertow) | [Undertow](https://undertow.io/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [NanoHttpdServer](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/transport/http/server/NanoHttpdServer.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |

### [Endpoint transport](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/EndpointMessageTransport.html)

Endpoint message transport protocol plugins.
  
Used to passively receive and reply to requests using specific message transport protocol from an active server while invoking RPC request handler to process them.

| Class | Artifact | Library | Protocol |
| ---- | --- | --- | --- |
| [UndertowEndpoint](https://www.javadoc.io/doc/io.automorph/automorph-undertow_2.13/latest/automorph/transport/http/endpoint/UndertowEndpoint.html) | [automorph-undertow](https://mvnrepository.com/artifact/io.automorph/automorph-undertow) | [Undertow](https://undertow.io/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [TapirEndpoint](https://www.javadoc.io/doc/io.automorph/automorph-tapir_2.13/latest/automorph/transport/http/endpoint/TapirEndpoint.html) | [automorph-tapir](https://mvnrepository.com/artifact/io.automorph/automorph-tapir) | [Tapir](https://tapir.softwaremill.com/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [JettyEndpoint](https://www.javadoc.io/doc/io.automorph/automorph-jetty_2.13/latest/automorph/transport/http/endpoint/JettyEndpoint.html) | [automorph-jetty](https://mvnrepository.com/artifact/io.automorph/automorph-jetty) | [Jetty](https://www.eclipse.org/jetty/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [FinagleEndpoint](https://www.javadoc.io/doc/io.automorph/automorph-finagle_2.13/latest/automorph/transport/http/endpoint/FinagleEndpoint.html) | [automorph-finagle](https://mvnrepository.com/artifact/io.automorph/automorph-finagle) | [Finagle](https://twitter.github.io/finagle/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |

# Architecture

## [Client](https://www.javadoc.io/doc/io.automorph/automorph-core_2.13/latest/automorph/Client.html)

The client provides automatic creation of transparent proxy instances for remote RPC endpoints defined by existing API classes. Additionally, it also
supports direct calls and notifications of remote API methods.

Depends on:

* [Effect system](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/EffectSystem.html)
* [Message format](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/MessageFormat.html)
* [Client message transport](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/ClientMessageTransport.html)

```
                  .--------.     .--------------------------.
                  | Client | --> | Client message transport |
                  '--------'     '--------------------------'
                   |     |             |
                   v     v             v
   .----------------.   .---------------.
   | Message format |   | Effect system |
   '----------------'   '---------------'
```

## Handler

The handler provides automatic creation of remote RPC endpoint bindings for existing API instances and subsequent processing RPC requests.

Depends on:

* [Effect system](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/EffectSystem.html)
* [Message format](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/MessageFormat.html)

```
   .--------------------------.     .---------.
   | Server message transport | --> | Handler |
   '--------------------------'     '---------'
                        |            |      |
                        v            v      v
                      .---------------.    .----------------.
                      | Effect system |    | Message format |
                      '---------------'    '----------------'
```

# Examples

## [Synchronous](/examples/src/test/scala/examples/Synchronous.scala)

### API

```scala
// Define an API type and create API instance
class Api {
  def hello(some: String, n: Int): String = s"Hello $some $n!"
}
val api = new Api()
```

### Server

```scala
  // Create and start RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer.sync(_.bind(api), 80, "/api")

// Stop the server
server.close()
```

### Client

```scala
// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val client = automorph.DefaultHttpClient.sync("http://localhost/api", "POST")

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // : String
```

## [Asynchronous](/examples/src/test/scala/examples/Asynchronous.scala)

### API

```scala
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Define an API type and create API instance
class Api {
  def hello(some: String, n: Int): Future[String] = Future.successful(s"Hello $some $n!")
}
val api = new Api()

```

### Server

```scala
// Create and start RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer.async(_.bind(api), 80, "/api")

// Stop the server
server.close()
```

### Client

```scala
// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val client = automorph.DefaultHttpClient.async("http://localhost/api", "POST")

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // : Future[String]
```

### Dynamic Client

```scala
// Call a remote API method dynamically passing the arguments by name
val hello = client.method("hello")
hello.args("some" -> "world", "n" -> 1).call[String] // Future[String]

// Call a remote API method dynamically passing the arguments by position
hello.positional.args("world", 1).call[String] // Future[String]

// Notify a remote API method dynamically passing the arguments by name
hello.args("some" -> "world", "n" -> 1).tell // Future[Unit]

// Notify a remote API method dynamically passing the arguments by position
hello.positional.args("world", 1).tell // Future[Unit]
```

## [Request context](/examples/src/test/scala/examples/RequestContext.scala)

### API

```scala
// Define server API type and create API instance
class ServerApi {
  import automorph.DefaultHttpServer.Context

  // Use request context provided by the server transport
  def requestMetaData(message: String)(implicit context: Context): List[String] =
    List(message, context.getRequestPath, context.getRequestHeaders.get("X-Test").peek)
}
val api = new ServerApi()

// Define client view of the server API
trait ClientApi {
  import automorph.DefaultHttpClient.Context

  // Supply request context used by the client transport
  def requestMetaData(message: String)(implicit context: Context): List[String]
}
```

### Server

```scala
// Create and start RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer.sync(_.bind(api), 80, "/api")

// Stop the server
server.close()
```

### Client

```scala
// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val client = automorph.DefaultHttpClient.sync("http://localhost/api", "POST")

// Create context for requests sent by the client
val apiProxy = client.bind[ClientApi] // Api
val defaultContext = client.defaultContext
implicit val context: automorph.DefaultHttpClient.Context = defaultContext.copy(
  partial = defaultContext.partial.header("X-Test", "valid")
)

// Call the remote API method via proxy
apiProxy.requestMetaData("test") // List("test", "/api", "valid")
apiProxy.requestMetaData("test")(context) // List("test", "/api", "valid")
client.method("requestMetaData").args("message" -> "test").call[List[String]] //  List("test", "/api", "valid")
```

## [Method alias](/examples/src/test/scala/examples/MethodAlias.scala)

### API

```scala
// Define an API type and create API instance
class Api {
  // Exposed as 'test.multiParams'
  def multiParams(add: Boolean)(n: Double): Double = if (add) n + 1 else n - 1

  // Exposed as 'original' and 'aliased'
  def original(value: Option[String]): String = value.getOrElse("")

  // Not exposed
  def omitted(): String = ""
}
val api = new Api()

// Define client view of the server API
trait ClientApi {
  import DefaultHttpClient.Context

  // Supply requets context used by the client transport
  def requestMetaData(message: String)(implicit context: Context): Future[List[String]]
}
```

### Server

```scala
// Customize method names
val mapMethodName = (name: String) => name match {
  case "original" => Seq("original", "aliased")
  case "omitted" => Seq()
  case other => Seq(s"test.$other")
}

// Create and start RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer.sync(_.bind(api, mapMethodName(_)), 80, "/api")

// Stop the server
server.close()
```

### Client

```scala
import scala.util.Try

// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val client = automorph.DefaultHttpClient.sync("http://localhost/api", "POST")

// Call the remote API method via proxy
client.method("test.multiParams").args("add" -> true, "n" -> 1).call[Double] // 2
client.method("aliased").args("value" -> None).tell // ()
Try(client.method("omitted").args().call[String]) // Failure
```

## [Choose effect system](/examples/src/test/scala/examples/ChooseEffectSystem.scala)

### Dependencies

```scala
libraryDependencies ++= Seq(
  "io.automorph" %% "automorph-default" % "1.0.0",
  "io.automorph" %% "automorph-zio" % "1.0.0",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % "3.3.9"
)
```

### API

```scala
import zio.{Runtime, Task}

// Define an API type and create API instance
class Api {
  def hello(some: String, n: Int): Task[String] = Task.succeed(s"Hello $some $n!")
}
val api = new Api()
```

### Server

```scala
import automorph.system.ZioSystem

// Custom effectful computation backend plugin
val system = ZioSystem[Any]()
val runEffect = (effect: Task[_]) => Runtime.default.unsafeRunTask(effect)

// Create and start RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer[ZioSystem.TaskEffect](system, runEffect, _.bind(api), 80, "/api")

// Stop the server
server.close()
```

### Client

```scala
import org.asynchttpclient.DefaultAsyncHttpClient
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend

// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val backend = AsyncHttpClientZioBackend.usingClient(Runtime.default, new DefaultAsyncHttpClient())
val client = automorph.DefaultHttpClient("http://localhost/api", "POST", system, backend)

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // : Task[String]
```

## [Choose message transport](/examples/src/test/scala/examples/ChooseMessageTransport.scala)

### API

```scala
// Define an API type and create API instance
class Api {
  def hello(some: String, n: Int): String = s"Hello $some $n!"
}
val api = new Api()
```

### Server

```scala
import automorph.system.IdentitySystem.Identity
import automorph.transport.http.server.NanoHttpdServer
import automorph.{Client, DefaultSystem, DefaultFormat, Handler}

// Create and start RPC server listening on port 80 for HTTP requests with URL path '/api'
val system = DefaultEffectSystem.sync
val runEffect = (effect: Identity[NanoHttpdServer.Response]) => effect
val format = DefaultMessageFormat()
val handler = Handler[DefaultMessageFormat.Node, format.type, Identity, NanoHttpdServer.Context](format, system)
val server = NanoHttpdServer(handler.bind(api), runEffect, 80)

// Stop the server
server.close()
```

### Client

```scala
import automorph.transport.http.client.UrlConnectionTransport
import java.net.URL

// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val transport = UrlConnectionClient(new URL("http://localhost/api"), "POST")
val client: Client[DefaultMessageFormat.Node, format.type, Identity, UrlConnectionClient.Context] =
  Client(format, system, transport)

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // : String
```

## [Choose message format](/examples/src/test/scala/examples/ChooseMessageFormat.scala)

### Dependencies

```scala
libraryDependencies ++= Seq(
  "io.automorph" %% "automorph-default" % "1.0.0",
  "io.automorph" %% "automorph-circe" % "1.0.0"
)
```

### API

```scala
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Define an API type and create API instance
case class Record(values: List[String])
class Api {
  def hello(some: String, n: Int): Future[Record] = Future.successful(Record(List("Hello", some, n.toString)))
}
val api = new Api()
```

### Server

```scala
import automorph.format.json.CirceJsonFormat
import automorph.transport.http.endpoint.UndertowJsonRpcHandler
import automorph.transport.http.server.UndertowServer
import automorph.{Client, DefaultSystem, DefaultHttpTransport, Handler}
import io.circe.generic.auto._

// Create and start RPC server listening on port 80 for HTTP requests with URL path '/api'
val system = DefaultEffectSystem.async
val runEffect = (effect: Future[_]) => effect
val format = CirceJsonFormat()
val handler = Handler[CirceJsonFormat.Node, format.type, Future, UndertowHandlerEndpoint.Context](format, system)
val server = UndertowServer(UndertowHandlerEndpoint(handler.bind(api), runEffect), 80, "/api")

// Stop the server
server.close()
```

### Client

```scala
  // Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val transport = DefaultHttpClientTransport.async("http://localhost/api", "POST")
val client: Client[CirceJsonFormat.Node, format.type, Future, DefaultHttpClientTransport.Context] =
  Client(format, system, transport)

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // : Future[String]
```

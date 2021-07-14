![automorph](https://github.com/martin-ockajak/automorph/raw/main/project/logo.jpg)

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
    - [Client message transport](#client-message-transport)
    - [Endpoint message transport](#endpoint-mesage-transport)
    - [Server message transport](#server-message-transport)
- [Architecture](#architecture)
- [Examples](#examples)
  - [Synchronous](#synchronous)
  - [Asynchronous](#asynchronous)
  - [Request context](#request-context)
  - [Method alias](#method-alias)
  - [Select effect system](#select-effect-system)
  - [Select message transport](#select-message-transport)
  - [Select message format](#select-message-format)

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
// Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer.async(_.bind(api), 80, "/api")

// Stop the server
server.close()
```

### Client

Invoke the API via JSON-RPC over HTTP(S).

```scala
// Create JSON-RPC client for sending HTTP POST requests to 'http://localhost/api'
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
| [FutureBackend](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/backend/FutureBackend.html) (Default) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard Scala](https://docs.scala-lang.org/overviews/core/futures.html) | [Future](https://www.scala-lang.org/api/current/scala/concurrent/Future.html) |
| [TryBackend](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/backend/TryBackend.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard Scala](https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html) | [Try](https://www.scala-lang.org/api/2.13.6/scala/util/Try.html) |
| [IdentityBackend](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/backend/IdentityBackend.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard Scala](https://www.scala-lang.org/) | [Identity](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/backend/IdentityBackend$$Identity.html) |
| [ZioBackend](https://www.javadoc.io/doc/io.automorph/automorph-zio_2.13/latest/automorph/backend/ZioBackend.html) | [automorph-zio](https://mvnrepository.com/artifact/io.automorph/automorph-zio) | [ZIO](https://zio.dev/) | [RIO](https://javadoc.io/doc/dev.zio/zio_2.13/latest/zio/RIO$.html) |
| [MonixBackend](https://www.javadoc.io/doc/io.automorph/automorph-monix_2.13/latest/automorph/backend/MonixBackend.html) | [automorph-monix](https://mvnrepository.com/artifact/io.automorph/automorph-monix) | [Monix](https://monix.io/) | [Task](https://monix.io/api/current/monix/eval/Task.html) |
| [CatsEffectBackend](https://www.javadoc.io/doc/io.automorph/automorph-cats-effect_2.13/latest/automorph/backend/CatsEffectBackend.html) | [automorph-cats-effect](https://mvnrepository.com/artifact/io.automorph/automorph-cats-effect) | [Cats Effect](https://typelevel.org/cats-effect/) | [IO](https://www.javadoc.io/doc/org.typelevel/cats-effect_3/latest/cats/effect/IO.html) |
| [ScalazBackend](https://www.javadoc.io/doc/io.automorph/automorph-scalaz_2.13/latest/automorph/backend/ScalazBackend.html) | [automorph-scalaz](https://mvnrepository.com/artifact/io.automorph/automorph-scalaz) | [Scalaz](https://github.com/scalaz) | [IO](https://www.javadoc.io/doc/org.scalaz/scalaz_2.13/latest/scalaz/effect/IO.html) |

## [Message format](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/MessageFormat.html)

Structured message format serialization/deserialization plugins.

The underlying format must support storing arbitrarily nested structures of basic data types.

| Class | Artifact | Library | Node Type |
| ---- | --- | --- | --- |
| [UpickleJsonCodec](https://www.javadoc.io/doc/io.automorph/automorph-upickle_2.13/latest/automorph/codec/json/UpickleJsonCodec.html) (Default) | [automorph-upickle](https://mvnrepository.com/artifact/io.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Value](http://com-lihaoyi.github.io/upickle/#uJson) |
| [UpickleMessagePackCodec](https://www.javadoc.io/doc/io.automorph/automorph-upickle_2.13/latest/automorph/codec/messagepack/UpickleMessagePackCodec.html) | [automorph-upickle](https://mvnrepository.com/artifact/io.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Msg](http://com-lihaoyi.github.io/upickle/#uPack) |
| [CirceJsonCodec](https://www.javadoc.io/doc/io.automorph/automorph-circe_2.13/latest/automorph/codec/json/CirceJsonCodec.html) | [automorph-circe](https://mvnrepository.com/artifact/io.automorph/automorph-circe) | [Circe](https://circe.github.io/circe) |[Json](https://circe.github.io/circe/api/io/circe/Json.html) |
| [ArgonautJsonCodec](https://www.javadoc.io/doc/io.automorph/automorph-argonaut_2.13/latest/automorph/codec/json/ArgonautJsonCodec.html) | [automorph-argonaut](https://mvnrepository.com/artifact/io.automorph/automorph-argonaut) | [Argonaut](http://argonaut.io/doc/) |[Json](http://argonaut.io/scaladocs/#argonaut.Json) |

## [Mesage transport](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/MessageTransport.html)

Message transport protocol plugins.

The underlying transport protocol must support implementation of request-response pattern.

### Client message transport

Client message transport protocol plugins.

Used by the RPC client to send requests and receive responses to and from a remote endpoint.

| Class | Artifact | Library | Protocol |
| ---- | --- | --- | --- |
| [UndertowJsonRpcHandler](https://www.javadoc.io/doc/io.automorph/automorph-undertow_2.13/latest/automorph/server/http/UndertowJsonRpcHandler.html) | [automorph-undertow](https://mvnrepository.com/artifact/io.automorph/automorph-undertow) | [Undertow](https://undertow.io/) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [TapirJsonRpcEndpoint](https://www.javadoc.io/doc/io.automorph/automorph-tapir_2.13/latest/automorph/server/http/TapirJsonRpcEndpoint.html) | [automorph-tapir](https://mvnrepository.com/artifact/io.automorph/automorph-tapir) | [Tapir](https://tapir.softwaremill.com/) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [JettyJsonRpcServlet](https://www.javadoc.io/doc/io.automorph/automorph-jetty_2.13/latest/automorph/server/http/JettyJsonRpcServlet.html) | [automorph-jetty](https://mvnrepository.com/artifact/io.automorph/automorph-jetty) | [Jetty](https://www.eclipse.org/jetty/) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [FinagleJsonRpcService](https://www.javadoc.io/doc/io.automorph/automorph-finagle_2.13/latest/automorph/server/http/FinagleJsonRpcService.html) | [automorph-finagle](https://mvnrepository.com/artifact/io.automorph/automorph-finagle) | [Finagle](https://twitter.github.io/finagle/) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |

## Server message transport

Server message transport protocol plugins.

Used to actively receive and reply to requests using specific message transport protocol
while invoking RPC request handler to process them.

| Class | Artifact | Library | Protocol |
| ---- | --- | --- | --- |
| [UndertowServer](https://www.javadoc.io/doc/io.automorph/automorph-undertow_2.13/latest/automorph/server/http/UndertowServer.html) (Default) | [automorph-undertow](https://mvnrepository.com/artifact/io.automorph/automorph-undertow) | [Undertow](https://undertow.io/) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [NanoHttpdServer](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/server/http/NanoHttpdServer.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |

## Endpoint message transport

Endpoint message transport protocol plugins.
  
Used to passively receive and reply to requests using specific message transport protocol from an active server while invoking RPC request handler to process them.

| Class | Artifact | Library | Protocol |
| ---- | --- | --- | --- |
| [UndertowJsonRpcHandler](https://www.javadoc.io/doc/io.automorph/automorph-undertow_2.13/latest/automorph/server/http/UndertowJsonRpcHandler.html) | [automorph-undertow](https://mvnrepository.com/artifact/io.automorph/automorph-undertow) | [Undertow](https://undertow.io/) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [TapirJsonRpcEndpoint](https://www.javadoc.io/doc/io.automorph/automorph-tapir_2.13/latest/automorph/server/http/TapirJsonRpcEndpoint.html) | [automorph-tapir](https://mvnrepository.com/artifact/io.automorph/automorph-tapir) | [Tapir](https://tapir.softwaremill.com/) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [JettyJsonRpcServlet](https://www.javadoc.io/doc/io.automorph/automorph-jetty_2.13/latest/automorph/server/http/JettyJsonRpcServlet.html) | [automorph-jetty](https://mvnrepository.com/artifact/io.automorph/automorph-jetty) | [Jetty](https://www.eclipse.org/jetty/) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [FinagleJsonRpcService](https://www.javadoc.io/doc/io.automorph/automorph-finagle_2.13/latest/automorph/server/http/FinagleJsonRpcService.html) | [automorph-finagle](https://mvnrepository.com/artifact/io.automorph/automorph-finagle) | [Finagle](https://twitter.github.io/finagle/) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |

# Architecture

## [Client](https://www.javadoc.io/doc/io.automorph/automorph-core_2.13/latest/automorph/Client.html)

The client provides automatic creation of transparent proxy instances for remote JSON-RPC endpoints defined by existing API classes. Additionally, it also
supports direct calls and notifications of remote API methods.

Depends on:

* [Backend](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/Backend.html)
* [Codec](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/Codec.html)
* [Transport](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/Transport.html)

```
        .--------.     .-----------.
        | Client | --> | Transport |
        '--------'     '-----------'
         |      |       |
         v      v       v
  .-------.    .---------.
  | Codec |    | Backend |
  '-------'    '---------'
```

## Handler

The handler provides automatic creation of remote JSON-RPC endpoint bindings for existing API instances and subsequent processing JSON-RPC requests.

Depends on:

* [Backend](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/Backend.html)
* [Codec](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/Codec.html)

```
     .--------.     .---------.
     | Server | --> | Handler |
     '--------'     '---------'
             |       |      |
             v       v      v
            .---------.    .-------.
            | Backend |    | Codec |
            '---------'    '-------'
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
  // Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer.sync(_.bind(api), 80, "/api")

// Stop the server
server.close()
```

### Client

```scala
// Create JSON-RPC client for sending HTTP POST requests to 'http://localhost/api'
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
// Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer.async(_.bind(api), 80, "/api")

// Stop the server
server.close()
```

### Client

```scala
// Create JSON-RPC client for sending HTTP POST requests to 'http://localhost/api'
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
// Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer.sync(_.bind(api), 80, "/api")

// Stop the server
server.close()
```

### Client

```scala
// Create JSON-RPC client for sending HTTP POST requests to 'http://localhost/api'
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

// Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer.sync(_.bind(api, mapMethodName(_)), 80, "/api")

// Stop the server
server.close()
```

### Client

```scala
import scala.util.Try

// Create JSON-RPC client for sending HTTP POST requests to 'http://localhost/api'
val client = automorph.DefaultHttpClient.sync("http://localhost/api", "POST")

// Call the remote API method via proxy
client.method("test.multiParams").args("add" -> true, "n" -> 1).call[Double] // 2
client.method("aliased").args("value" -> None).tell // ()
Try(client.method("omitted").args().call[String]) // Failure
```

## [Select effect system](/examples/src/test/scala/examples/SelectEffectSystem.scala)

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
import automorph.backend.ZioBackend

// Create computational effect system
val backend = automorph.backend.ZioBackend[Any]()
val runEffect = (effect: Task[_]) => Runtime.default.unsafeRunTask(effect)

// Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer[ZioBackend.TaskEffect](backend, runEffect, _.bind(api), 80, "/api")

// Stop the server
server.close()
```

### Client

```scala
import org.asynchttpclient.DefaultAsyncHttpClient
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend

// Create JSON-RPC client for sending HTTP POST requests to 'http://localhost/api'
val sttpBackend = AsyncHttpClientZioBackend.usingClient(Runtime.default, new DefaultAsyncHttpClient())
val client = automorph.DefaultHttpClient("http://localhost/api", "POST", backend, sttpBackend)

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // : Task[String]
```

## [Select message transport](/examples/src/test/scala/examples/SelectMessageTransport.scala)

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
import automorph.backend.IdentityBackend.Identity
import automorph.transport.http.server.NanoHttpdServer
import automorph.{Client, DefaultBackend, DefaultCodec, Handler}

// Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
val backend = DefaultBackend.sync
val runEffect = (effect: Identity[NanoHttpdServer.Response]) => effect
val codec = DefaultCodec()
val handler = Handler[DefaultCodec.Node, codec.type, Identity, NanoHttpdServer.Context](codec, backend)
val server = NanoHttpdServer(handler.bind(api), runEffect, 80)

// Stop the server
server.close()
```

### Client

```scala
import automorph.transport.http.client.UrlConnectionTransport
import java.net.URL

// Create JSON-RPC client for sending HTTP POST requests to 'http://localhost/api'
val transport = UrlConnectionTransport(new URL("http://localhost/api"), "POST")
val client: Client[DefaultCodec.Node, codec.type, Identity, UrlConnectionTransport.Context] =
  Client(codec, backend, transport)

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // : String
```

## [Select message format](/examples/src/test/scala/examples/SelectMessageFormat.scala)

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
import automorph.codec.json.CirceJsonCodec
import automorph.transport.http.endpoint.UndertowJsonRpcHandler
import automorph.transport.http.server.UndertowServer
import automorph.{Client, DefaultBackend, DefaultHttpTransport, Handler}
import io.circe.generic.auto._

// Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
val backend = DefaultBackend.async
val runEffect = (effect: Future[_]) => effect
val codec = CirceJsonCodec()
val handler = Handler[CirceJsonCodec.Node, codec.type, Future, UndertowJsonRpcHandler.Context](codec, backend)
val server = UndertowServer(UndertowJsonRpcHandler(handler.bind(api), runEffect), 80, "/api")

// Stop the server
server.close()
```

### Client

```scala
// Create JSON-RPC client for sending HTTP POST requests to 'http://localhost/api'
val clientTransport = DefaultHttpTransport.async("http://localhost/api", "POST")
val client: Client[CirceJsonCodec.Node, codec.type, Future, DefaultHttpTransport.Context] =
  Client(codec, backend, clientTransport)

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // : Future[String]
```

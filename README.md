![automorph](https://github.com/martin-ockajak/automorph/raw/main/project/logo.jpg)

**Automorph** is a [JSON-RPC](https://www.jsonrpc.org/specification) client and server library for [Scala](https://www.scala-lang.org/) providing an effortless
way to invoke and expose remote APIs.

[![Build](https://github.com/martin-ockajak/automorph/workflows/Build/badge.svg)](https://github.com/martin-ockajak/automorph/actions/workflows/tests.yml)
[![Releases](https://img.shields.io/maven-central/v/io.automorph/automorph-core_2.13.svg)](https://mvnrepository.com/artifact/io.automorph)
[![Scaladoc](https://javadoc-badge.appspot.com/io.automorph/automorph-core_2.13.svg?label=scaladoc)](https://javadoc.io/doc/io.automorph/automorph-core_2.13/latest/automorph/)

- [Features](#features)
- [Quickstart](#quickstart)
  - [Build](#build)
  - [API](#api)
  - [Server](#server)
  - [Client](#client)
  - [Dynamic Client](#dynamic-client)
- [Integration](#integration)
  - [Effect backend](#backend)
  - [Message codec](#codec)
  - [Message transport](#transport)
- [Documentation](#documentation)
  - [Architecture](#architecture)
  - [Scaladoc](#scaladoc)
  - [Inspiration](#inspiration)
- [Examples](#examples)
  - [Synchronous](#synchronous)
  - [Asynchronous](#asynchronous)
  - [Custom effect backend](#custom-effect-backend)
  - [Custom message transport](#custom-message-transport)

# Features

* **Powerful** - generate JSON-RPC bindings directly from public methods of your API classes
* **Modular** - combine integration plugins to match your API effect type, message format and transport protocol
* **Clean** - access underlying protocol details without polluting your API abstractions
* **Safe** - type checks bound API classes during compilation
* **Fast** - generates optimized API binding code during compilation
* **Flexible** - customize JSON-RPC method names and error codes
* **Extensible** - automatically derive or manually implement serialization of arbitrary data types
* **Compatible** - available for [Scala 2.12](https://www.scala-lang.org/news/2.12.0/), [Scala 2.13](https://www.scala-lang.org/news/2.13.0)
  and [Scala 3](https://dotty.epfl.ch/)
* **Boilerplate free** - even advanced use-cases require only a few lines of code
* **Dependency free** - core logic depends on [SLF4J API](http://www.slf4j.org/) only

# Quickstart

Exposing and invoking a JSON-RPC API using HTTP as transport protocol.


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

## [Backend](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/Backend.html)

Effectful computation plugins:

| Class | Artifact | Library | Effect Type |
| ---- | --- | --- | --- |
| [FutureBackend](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/backend/FutureBackend.html) (Default) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard Scala](https://docs.scala-lang.org/overviews/core/futures.html) | [Future](https://www.scala-lang.org/api/current/scala/concurrent/Future.html) |
| [TryBackend](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/backend/TryBackend.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard Scala](https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html) | [Try](https://www.scala-lang.org/api/2.13.6/scala/util/Try.html) |
| [IdentityBackend](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/backend/IdentityBackend.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard Scala](https://www.scala-lang.org/) | [Identity](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/backend/IdentityBackend$$Identity.html) |
| [ZioBackend](https://www.javadoc.io/doc/io.automorph/automorph-zio_2.13/latest/automorph/backend/ZioBackend.html) | [automorph-zio](https://mvnrepository.com/artifact/io.automorph/automorph-zio) | [ZIO](https://zio.dev/) | [RIO](https://javadoc.io/doc/dev.zio/zio_2.13/latest/zio/RIO$.html) |
| [MonixBackend](https://www.javadoc.io/doc/io.automorph/automorph-monix_2.13/latest/automorph/backend/MonixBackend.html) | [automorph-monix](https://mvnrepository.com/artifact/io.automorph/automorph-monix) | [Monix](https://monix.io/) | [Task](https://monix.io/api/current/monix/eval/Task.html) |
| [CatsEffectBackend](https://www.javadoc.io/doc/io.automorph/automorph-cats-effect_2.13/latest/automorph/backend/CatsEffectBackend.html) | [automorph-cats-effect](https://mvnrepository.com/artifact/io.automorph/automorph-cats-effect) | [Cats Effect](https://typelevel.org/cats-effect/) | [IO](https://www.javadoc.io/doc/org.typelevel/cats-effect_3/latest/cats/effect/IO.html) |
| [ScalazBackend](https://www.javadoc.io/doc/io.automorph/automorph-scalaz_2.13/latest/automorph/backend/ScalazBackend.html) | [automorph-scalaz](https://mvnrepository.com/artifact/io.automorph/automorph-scalaz) | [Scalaz](https://github.com/scalaz) | [IO](https://www.javadoc.io/doc/org.scalaz/scalaz_2.13/latest/scalaz/effect/IO.html) |

## [Codec](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/Codec.html)

Hierarchical message data format plugins:

| Class | Artifact | Library | Node Type |
| ---- | --- | --- | --- |
| [UpickleJsonCodec](https://www.javadoc.io/doc/io.automorph/automorph-upickle_2.13/latest/automorph/codec/json/UpickleJsonCodec.html) (Default) | [automorph-upickle](https://mvnrepository.com/artifact/io.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Value](http://com-lihaoyi.github.io/upickle/#uJson) |
| [UpickleMessagePackCodec](https://www.javadoc.io/doc/io.automorph/automorph-upickle_2.13/latest/automorph/codec/messagepack/UpickleMessagePackCodec.html) | [automorph-upickle](https://mvnrepository.com/artifact/io.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Msg](http://com-lihaoyi.github.io/upickle/#uPack) |
| [CirceJsonCodec](https://www.javadoc.io/doc/io.automorph/automorph-circe_2.13/latest/automorph/codec/json/CirceJsonCodec.html) | [automorph-circe](https://mvnrepository.com/artifact/io.automorph/automorph-circe) | [Circe](https://circe.github.io/circe) |[Json](https://circe.github.io/circe/api/io/circe/Json.html) |
| [ArgonautJsonCodec](https://www.javadoc.io/doc/io.automorph/automorph-argonaut_2.13/latest/automorph/codec/json/ArgonautJsonCodec.html) | [automorph-argonaut](https://mvnrepository.com/artifact/io.automorph/automorph-argonaut) | [Argonaut](http://argonaut.io/doc/) |[Json](http://argonaut.io/scaladocs/#argonaut.Json) |

## [Transport](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/Transport.html)

Message transport plugins:

| Class | Artifact | Library | Protocol | Type |
| ---- | --- | --- | --- | --- |
| [UndertowServer](https://www.javadoc.io/doc/io.automorph/automorph-undertow_2.13/latest/automorph/server/http/UndertowServer.html) (Default) | [automorph-undertow](https://mvnrepository.com/artifact/io.automorph/automorph-undertow) | [Undertow](https://undertow.io/) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) | Server |
| [UndertowJsonRpcHandler](https://www.javadoc.io/doc/io.automorph/automorph-undertow_2.13/latest/automorph/server/http/UndertowJsonRpcHandler.html) | [automorph-undertow](https://mvnrepository.com/artifact/io.automorph/automorph-undertow) | [Undertow](https://undertow.io/) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) | Endpoint |
| [TapirJsonRpcEndpoint](https://www.javadoc.io/doc/io.automorph/automorph-tapir_2.13/latest/automorph/server/http/TapirJsonRpcEndpoint.html) | [automorph-tapir](https://mvnrepository.com/artifact/io.automorph/automorph-tapir) | [Tapir](https://tapir.softwaremill.com/) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) | Endpoint |
| [JettyJsonRpcServlet](https://www.javadoc.io/doc/io.automorph/automorph-jetty_2.13/latest/automorph/server/http/JettyJsonRpcServlet.html) | [automorph-jetty](https://mvnrepository.com/artifact/io.automorph/automorph-jetty) | [Jetty](https://www.eclipse.org/jetty/) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) | Endpoint |
| [FinagleJsonRpcService](https://www.javadoc.io/doc/io.automorph/automorph-finagle_2.13/latest/automorph/server/http/FinagleJsonRpcService.html) | [automorph-finagle](https://mvnrepository.com/artifact/io.automorph/automorph-finagle) | [Finagle](https://twitter.github.io/finagle/) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) | Endpoint |
| [NanoHttpdServer](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/server/http/NanoHttpdServer.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) | Server |
| [SttpTransport](https://www.javadoc.io/doc/io.automorph/automorph-sttp_2.13/latest/automorph/transport/http/SttpTransport.html) (Default) | [automorph-sttp](https://mvnrepository.com/artifact/io.automorph/automorph-sttp) | [STTP](https://sttp.softwaremill.com/en/latest/) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) | Client |
| [UrlConnectionTransport](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/transport/http/UrlConnectionTransport.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard Scala](https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html) |[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) | Client |

# Documentation

## Architecture

### [Client](https://www.javadoc.io/doc/io.automorph/automorph-core_2.13/latest/automorph/Client.html)

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

### Handler

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

### [Scaladoc](https://www.javadoc.io/doc/io.automorph/automorph-core_2.13/latest/)

### Inspiration

* [Scala JSON-RPC](https://github.com/shogowada/scala-json-rpc)
* [Autowire](https://github.com/lihaoyi/autowire)
* [STTP](https://github.com/softwaremill/sttp)

# Examples

## Synchronous

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

## Asynchronous

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

## Custom effect backend

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
// Custom effectful computation backend plugin
val backend = automorph.backend.ZioBackend[Any]()
val runEffect = (effect: Task[_]) => Runtime.default.unsafeRunTask(effect)

// Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer(backend, runEffect, _.bind(api), 80, "/api")

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

## Custom message transport

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
val transport = UrlConnectionTransport(new URL("http://localhost/api"), "POST")
val client = automorph.Client(automorph.DefaultCodec(), automorph.DefaultBackend.sync, transport)

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // : Future[String]
```

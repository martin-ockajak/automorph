# Overview

**Automorph** is a [Scala](https://www.scala-lang.org/) [JSON-RPC](https://www.jsonrpc.org/specification) client and server library for effortlessly invoking
and exposing remote APIs.

# Features

* **Simple** - generate JSON-RPC bindings automatically from public methods of existing API classes
* **Extensible** - automatically derive or implement serialization logic for arbitrary data types
* **Modular** - freely combine various plugins to integrate with other libraries
* **Flexible** - customize mapping of JSON-RPC method names and error codes
* **Type safe** - validate bound API classes during compilation
* **Performant** - generate optimized API binding code during compilation
* **Compatible** - support for [Scala 2.12](https://www.scala-lang.org/news/2.12.0/), [Scala 2.13](https://www.scala-lang.org/news/2.13.0) and [Scala 3](https://dotty.epfl.ch/)
* **No boilerplate** - even advanced use requires only a few lines of code
* **No dependencies** - core implementation depends on [SLF4J API](http://www.slf4j.org/) only

# Plugins

Plugins between different published artifacts to reduce the required external dependencies.

## Backend

Effectful computation plugins:

| Class | Artifact | Library | Effect Type |
| ---- | --- | --- | --- |
| [IdentityBackend](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/backend/IdentityBackend.html)  | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard Library](https://www.scala-lang.org/) | *Identity* |
| [FutureBackend](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/backend/FutureBackend.html)  | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard Library](https://docs.scala-lang.org/overviews/core/futures.html) | [Future](https://www.scala-lang.org/api/current/scala/concurrent/Future.html) |
| [TryBackend](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/backend/TryBackend.html)  | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard Library](https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html) | [Try](https://www.scala-lang.org/api/2.13.6/scala/util/Try.html) |
| [ZioBackend](https://www.javadoc.io/doc/io.automorph/automorph-zio_2.13/latest/automorph/backend/ZioBackend.html)  | [automorph-zio](https://mvnrepository.com/artifact/io.automorph/automorph-zio) | [ZIO](https://zio.dev/) | [RIO](https://javadoc.io/doc/dev.zio/zio_2.13/latest/zio/RIO$.html) |
| [MonixBackend](https://www.javadoc.io/doc/io.automorph/automorph-monix_2.13/latest/automorph/backend/MonixBackend.html)  | [automorph-monix](https://mvnrepository.com/artifact/io.automorph/automorph-monix) | [Monix](https://monix.io/) | [Task](https://monix.io/api/current/monix/eval/Task.html) |
| [CatsEffectBackend](https://www.javadoc.io/doc/io.automorph/automorph-cats-effect_2.13/latest/automorph/backend/CatsEffectBackend.html)  | [automorph-cats-effect](https://mvnrepository.com/artifact/io.automorph/automorph-cats-effect) | [Cats](https://typelevel.org/cats-effect/) | [IO](https://www.javadoc.io/doc/org.typelevel/cats-effect_3/latest/cats/effect/IO.html) |
| [ScalazBackend](https://www.javadoc.io/doc/io.automorph/automorph-scalaz_2.13/latest/automorph/backend/ScalazBackend.html)  | [automorph-scalaz](https://mvnrepository.com/artifact/io.automorph/automorph-scalaz) | [Scalaz](https://github.com/scalaz) | [IO](https://www.javadoc.io/doc/org.scalaz/scalaz_2.13/latest/scalaz/effect/IO.html) |

## Codec

Hierarchical message data format plugins:

| Class | Artifact | Library | Node Type |
| ---- | --- | --- | --- |
| [UpickleJsonCodec](https://www.javadoc.io/doc/io.automorph/automorph-upickle_2.13/latest/automorph/codec/json/UpickleJsonCodec.html)  |  [automorph-upickle](https://mvnrepository.com/artifact/io.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Value](http://com-lihaoyi.github.io/upickle/#uJson) |
| [UpickleMessagePackCodec](https://www.javadoc.io/doc/io.automorph/automorph-upickle_2.13/latest/automorph/codec/messagepack/UpickleMessagePackCodec.html)  |  [automorph-upickle](https://mvnrepository.com/artifact/io.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Msg](http://com-lihaoyi.github.io/upickle/#uPack) |
| [CirceJsonCodec](https://www.javadoc.io/doc/io.automorph/automorph-circe_2.13/latest/automorph/codec/json/CirceJsonCodec.html)  |  [automorph-circe](https://mvnrepository.com/artifact/io.automorph/automorph-circe) | [Circe](https://circe.github.io/circe) |[Json](https://circe.github.io/circe/api/io/circe/Json.html) |
| [ArgonautJsonCodec](https://www.javadoc.io/doc/io.automorph/automorph-argonaut_2.13/latest/automorph/codec/json/ArgonautJsonCodec.html)  |  [automorph-argonaut](https://mvnrepository.com/artifact/io.automorph/automorph-argonaut) | [Argonaut](http://argonaut.io/doc/) |[Json](http://argonaut.io/scaladocs/#argonaut.Json) |

## Transport

Message transport plugins:

## Server

Remote endpoint server plugins:

# Quick Start

JSON-RPC over HTTP.

## Build

Add the following to your `build.sbt` file:

```scala
libraryDependencies += "io.automorph" %% "automorph-default" % "1.0.0"
```

## API

Define an *API* class:

```scala
import scala.concurrent.Future

class Api {
  def hello(thing: String): Future[String] = Future.succesful(s"Hello $thing!")
}

val api = new Api()

```

## Server

Expose the remote *API*:

```scala
// Create and start the server
val server = automorph.DefaultHttpServer.async(_.bind(api), 80, "/api")

// Stop the server
server.close()
```

## Client

Invoke the remote *API*:

```scala
// Create the client
val client = automorph.DefaultHttpClient.async("http://localhost/api", "POST")

// Proxy call
val apiProxy = client.bindByName[Api]
val proxyResult = apiProxy.hello("world") // Future[String]

// Direct call
val directResult: Future[String] = client.callByName("hello", "world")

// Direct notification
client.notifyByName("hello", "world") // Future[Unit]

```

# Architecture

## Client

The client provides automatic creation of transparent proxy instances for remote JSON-RPC endpoints defined by existing API classes. Additionally, it also supports direct calls and notifications of remote API methods.

Client plugins:

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

Handler plugins:

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

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

| Name | Effect Type | Artifact | Library |
| ---- | --- | --- | --- |
| **IdentityBackend**  | *Identity* | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard Library](https://www.scala-lang.org/) |
| **FutureBackend**  | [Future](https://www.scala-lang.org/api/current/scala/concurrent/Future.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard Library](https://docs.scala-lang.org/overviews/core/futures.html) |
| **TryBackend**  | [Try](https://www.scala-lang.org/api/2.13.6/scala/util/Try.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard Library](https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html) |
| **ZioBackend**  | [RIO](https://javadoc.io/doc/dev.zio/zio_2.13/latest/zio/RIO$.html) | [automorph-zio](https://mvnrepository.com/artifact/io.automorph/automorph-zio) | [ZIO](https://zio.dev/) |
| **MonixBackend**  | [Task](https://monix.io/api/current/monix/eval/Task.html) | [automorph-monix](https://mvnrepository.com/artifact/io.automorph/automorph-monix) | [Monix](https://monix.io/) |
| **CatsEffectBackend**  | [IO](https://www.javadoc.io/doc/org.typelevel/cats-effect_3/latest/cats/effect/IO.html) | [automorph-cats-effect](https://mvnrepository.com/artifact/io.automorph/automorph-cats-effect) | [Cats](https://typelevel.org/cats-effect/) |
| **ScalazBackend**  | [IO](https://www.javadoc.io/doc/org.scalaz/scalaz_2.13/7.4.0/scalaz/effect/IO.html) | [automorph-scalaz](https://mvnrepository.com/artifact/io.automorph/automorph-scalaz-effect) | [Scalaz](https://github.com/scalaz) |

## Codec

Hierarchical message data format plugins:

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
val server = io.automorph.DefaultHttpServer.async(_.bind(api), 80, "/api")

// Stop the server
server.close()
```

## Client

Invoke the remote *API*:

```scala
// Create the client
val client = io.automorph.DefaultHttpClient.async("http://localhost/api", "POST")

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

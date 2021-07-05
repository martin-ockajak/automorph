# Overview

**Automorph** is a [Scala](https://www.scala-lang.org/) [JSON-RPC](https://www.jsonrpc.org/specification) client and server library for effortlessly invoking
and exposing remote APIs.

# Features

* **Simple** - automatically generate JSON-RPC bindings for public methods of existing API classes
* **Extensible** - support serialization of arbitrary data types
* **Modular** - combine effect **backend**, data format **codec**, message **transport** and endpoint **server** plugins to suit specific needs
* **Flexible** - customize JSON-RPC method name and error code mapping
* **Type safe** - validate bound API classes during compilation
* **Performant** - generate optimized API binding code during compilation
* **Compatible** - full support of JSON-RPC 2.0 specification
* **No boilerplate** - even advanced use-cases require only a few lines of code
* **No dependencies** - core implementation depends on [SLF4J API](http://www.slf4j.org/) only

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

# Plugins

## Backend

## Codec

## Transport

## Server

# Examples

![automorph](https://github.com/martin-ockajak/automorph/raw/main/project/images/logo.jpg)

[![Build](https://github.com/martin-ockajak/automorph/workflows/Build/badge.svg)](https://github.com/martin-ockajak/automorph/actions/workflows/tests.yml)
[![Releases](https://img.shields.io/maven-central/v/io.automorph/automorph-core_2.13.svg)](https://mvnrepository.com/artifact/io.automorph)
[![Scaladoc](https://javadoc-badge.appspot.com/io.automorph/automorph-core_2.13.svg?label=scaladoc)](https://javadoc.io/doc/io.automorph/automorph-core_2.13/latest/automorph/)

**Automorph** is an [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) client and server library for [Scala](https://www.scala-lang.org/) providing an effortless
way to invoke and expose remote APIs using [JSON-RPC](https://www.jsonrpc.org/specification) and [REST-RPC](https://en.wikipedia.org/wiki/Representational_state_transfer) protocols.

- [Overview](#overview)
  - [Goals](#goals)
  - [Standards](#standards)
  - [Features](#features)
  - [Inspiration](#inspiration)
- [Integration](#integration)
  - [Effect system](#effect-system)
  - [Message format](#message-format)
  - [Message transport](#message-transport)
    - [Client transport](#client-transport)
    - [Endpoint transport](#endpoint-transport)
    - [Server transport](#server-transport)
- [Architecture](#architecture)

# Overview

## Goals

* Provide a **definitive RPC solution** for Scala ecosystem
* Strive for **easiest possible integration** with existing applications
* Encourage use of **appropriate technical standards** for system interoperability

## Standards

* [JSON-RPC](https://www.jsonrpc.org/specification)
* [REST-RPC](https://en.wikipedia.org/wiki/Representational_state_transfer)
* [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol)
* [WebSocket](https://en.wikipedia.org/wiki/WebSocket)
* [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol)
* [JSON](https://www.json.org/)
* [MessagePack](https://msgpack.org/)

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
| [CirceJsonFormat](https://www.javadoc.io/doc/io.automorph/automorph-circe_2.13/latest/automorph/format/json/CirceJsonFormat.html) (Default) | [automorph-circe](https://mvnrepository.com/artifact/io.automorph/automorph-circe) | [Circe](https://circe.github.io/circe) |[Json](https://circe.github.io/circe/api/io/circe/Json.html) | [JSON](https://www.json.org/) |
| [UpickleJsonFormat](https://www.javadoc.io/doc/io.automorph/automorph-upickle_2.13/latest/automorph/format/json/UpickleJsonFormat.html) | [automorph-upickle](https://mvnrepository.com/artifact/io.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Value](http://com-lihaoyi.github.io/upickle/#uJson) | [JSON](https://www.json.org/) |
| [UpickleMessagePackFormat](https://www.javadoc.io/doc/io.automorph/automorph-upickle_2.13/latest/automorph/format/messagepack/UpickleMessagePackFormat.html) | [automorph-upickle](https://mvnrepository.com/artifact/io.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Msg](http://com-lihaoyi.github.io/upickle/#uPack) | [MessagePack](https://msgpack.org/) |
| [ArgonautJsonFormat](https://www.javadoc.io/doc/io.automorph/automorph-argonaut_2.13/latest/automorph/format/json/ArgonautJsonFormat.html) | [automorph-argonaut](https://mvnrepository.com/artifact/io.automorph/automorph-argonaut) | [Argonaut](http://argonaut.io/doc/) |[Json](http://argonaut.io/scaladocs/#argonaut.Json) | [JSON](https://www.json.org/) |

## [Message transport](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/MessageTransport.html)

Message transport protocol plugins.

The underlying transport protocol must support request/response messaging pattern.

### [Client transport](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/ClientMessageTransport.html)

Client message transport protocol plugins.

Used by the RPC client to send requests and receive responses to and from a remote endpoint.

| Class | Artifact | Library | Effect Type | Protocol |
| ---- | --- | --- | --- | --- |
| [SttpClient](https://www.javadoc.io/doc/io.automorph/automorph-sttp_2.13/latest/automorph/transport/http/client/SttpClient.html) (Default) | [automorph-sttp](https://mvnrepository.com/artifact/io.automorph/automorph-sttp) | [STTP](https://sttp.softwaremill.com/en/latest/) -> [Akka HTTP, AsyncHttpClient, HttpClient, OkHttp](https://sttp.softwaremill.com/en/latest/backends/summary.html)| *All* | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [HttpUrlConnectionClient](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/transport/http/client/HttpUrlConnectionClient.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard](https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html) | [Identity](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/system/IdentitySystem$$Identity.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [RabbitMqClient](https://www.javadoc.io/doc/io.automorph/automorph-rabbitmq_2.13/latest/automorph/transport/amqp/client/RabbitMqClient.html) | [automorph-rabbitmq](https://mvnrepository.com/artifact/io.automorph/automorph-rabbitmq) | [RabbitMq](https://www.rabbitmq.com/java-client.html) | [Future](https://www.scala-lang.org/api/current/scala/concurrent/Future.html) | [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol) |

### [Server transport](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/ServerMessageTransport.html)

Server message transport protocol plugins.

Used to actively receive and reply to requests using specific message transport protocol
while invoking RPC request handler to process them.

| Class | Artifact | Library | Effect Type | Protocol |
| ---- | --- | --- | --- | --- |
| [UndertowServer](https://www.javadoc.io/doc/io.automorph/automorph-undertow_2.13/latest/automorph/transport/http/server/UndertowServer.html) (Default) | [automorph-undertow](https://mvnrepository.com/artifact/io.automorph/automorph-undertow) | [Undertow](https://undertow.io/) | *All* | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [NanoHttpdServer](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/transport/http/server/NanoHttpdServer.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd) | *All* | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [RabbitMqServer](https://www.javadoc.io/doc/io.automorph/automorph-rabbitmq_2.13/latest/automorph/transport/amqp/server/RabbitMqServer.html) | [automorph-rabbitmq](https://mvnrepository.com/artifact/io.automorph/automorph-rabbitmq) | [RabbitMq](https://www.rabbitmq.com/java-client.html) | *All* | [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol) |

### [Endpoint transport](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/EndpointMessageTransport.html)

Endpoint message transport protocol plugins.
  
Used to passively receive and reply to requests using specific message transport protocol from an active server while invoking RPC request handler to process them.

| Class | Artifact | Library | Effect Type | Protocol |
| ---- | --- | --- | --- | --- |
| [UndertowHttpEndpoint](https://www.javadoc.io/doc/io.automorph/automorph-undertow_2.13/latest/automorph/transport/http/endpoint/UndertowHttpEndpoint.html) | [automorph-undertow](https://mvnrepository.com/artifact/io.automorph/automorph-undertow) | [Undertow](https://undertow.io/) | *All* | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [UndertowWebSocketEndpoint](https://www.javadoc.io/doc/io.automorph/automorph-undertow_2.13/latest/automorph/transport/websocket/endpoint/UndertowWebSocketEndpoint.html) | [automorph-undertow](https://mvnrepository.com/artifact/io.automorph/automorph-undertow) | [Undertow](https://undertow.io/) | *All* | [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [TapirHttpEndpoint](https://www.javadoc.io/doc/io.automorph/automorph-tapir_2.13/latest/automorph/transport/http/endpoint/TapirHttpEndpoint.html) | [automorph-tapir](https://mvnrepository.com/artifact/io.automorph/automorph-tapir) | [Tapir](https://tapir.softwaremill.com/) -> [Akka HTTP](https://tapir.softwaremill.com/en/latest/server/akkahttp.html), [Finatra](https://tapir.softwaremill.com/en/latest/server/finatra.html), [http4s](https://tapir.softwaremill.com/en/latest/server/http4s.html), [Play](https://tapir.softwaremill.com/en/latest/server/play.html), [Vert.X](https://tapir.softwaremill.com/en/latest/server/vertx.html), [ZIO Http](https://tapir.softwaremill.com/en/latest/server/ziohttp.html) | *All* | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [JettyEndpoint](https://www.javadoc.io/doc/io.automorph/automorph-jetty_2.13/latest/automorph/transport/http/endpoint/JettyEndpoint.html) | [automorph-jetty](https://mvnrepository.com/artifact/io.automorph/automorph-jetty) | [Jetty](https://www.eclipse.org/jetty/) | *All* | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [FinagleEndpoint](https://www.javadoc.io/doc/io.automorph/automorph-finagle_2.13/latest/automorph/transport/http/endpoint/FinagleEndpoint.html) | [automorph-finagle](https://mvnrepository.com/artifact/io.automorph/automorph-finagle) | [Finagle](https://twitter.github.io/finagle/) | *All* | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |

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


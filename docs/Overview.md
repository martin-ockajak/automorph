---
sidebar_position: 1
---

# Overview

**Automorph** is an [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) client and server library for [Scala](https://www.scala-lang.org/) providing an effortless way to invoke and expose remote APIs using [JSON-RPC](https://www.jsonrpc.org/specification) and [Web-RPC](Web-RPC) protocols.


## Goals

* Enable consuming and exposing **remote APIs** transparently **without** needing to create **intermediate layer**
* Preserve user's **freedom** to make **technical decisions** and **access transport protocol** metadata
* Strive for **smooth integration** with other **libraries** and existing **applications**


## API

Entry points for the application logic to invoke or expose remote APIs:

* [Client](/api/automorph/Client.html) - call type-safe remote APIs (*bind & call*)
* [Handler](/api/automorph/Handler.html) - generate remote call bindings for existing APIs (*bind & process*)
* [Servers](/api/automorph/transport/index.html) - serve existing APIs remotely (*bind & serve*)


## SPI

Interfaces for implementation of various integration plugins:

* [EffectSystem](/api/automorph/spi/EffectSystem.html) - invoking and exposing remote APIs using various effect handling abstractions
* [MessageCodec](/api/automorph/spi/MessageCodec.html) - serialization of RPC messages into structured data formats
* [MessageTransport](/api/automorph/spi/MessageTransport.html) - transfer of RPC messages via different transport protocols
* [RpcProtocol](/api/automorph/spi/RpcProtocol.html) - specific RPC protocol support


## Limitations

* Bound remote APIs cannot contain [overloaded methods][https://en.wikipedia.org/wiki/Function_overloading]
* Bound remote API methods cannot use [type parameters](https://docs.scala-lang.org/tour/polymorphic-methods.html)
* Remote APIs cannot be invoked or exposed from within the [App](https://scala-lang.org/api/3.x/scala/App.html) trait nor any other form of [delayed initialization](https://scala-lang.org/api/3.x/scala/DelayedInit.html)
* Due to Scala 2 type inference constraints it is sometimes necessary to explicitly supplying type parameters when composing plugins


## Supported standards

### RPC protocols

* [JSON-RPC](https://www.jsonrpc.org/specification) (*Default*)
* [Web-RPC](Web-RPC)

### Effect Handling

* [Synchronous](https://docs.scala-lang.org/scala3/book/taste-functions.html) (*Default*)
* [Asynchronous](https://docs.scala-lang.org/overviews/core/futures.html) (*Default*)
* [Monadic](https://blog.softwaremill.com/figuring-out-scala-functional-programming-libraries-af8230efccb4)

### Transport protocols

* [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) (*Default*)
* [WebSocket](https://en.wikipedia.org/wiki/WebSocket)
* [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol)

### Message formats

* [JSON](https://www.json.org) (*Default*)
* [MessagePack](https://msgpack.org)

### API schemas

* [OpenRPC](https://spec.open-rpc.org)
* [OpenAPI](https://github.com/OAI/OpenAPI-Specification)


## Inspired by

* [Scala JSON-RPC](https://github.com/shogowada/scala-json-rpc)
* [Autowire](https://github.com/lihaoyi/autowire)
* [http4s](https://http4s.org)
* [STTP](https://sttp.softwaremill.com)
* [ZIO](https://zio.dev)


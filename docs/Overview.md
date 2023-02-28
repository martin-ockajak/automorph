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

* [Client](/api/automorph/Client.html) - invoke remote APIs
* [Handler](/api/automorph/Handler.html) - process remote API requests
* [Endpoints](/api/automorph/transport/EndpointTransport.html) - expose APIs as remote within an existing server
* [Servers](/api/automorph/transport/ServerTransport.html) - serve APIs as remote


## SPI

Interfaces for implementation of various integration plugins:

* [EffectSystem](/api/automorph/spi/EffectSystem.html) - accessing remote APIs using various effect handling abstractions
* [MessageCodec](/api/automorph/spi/MessageCodec.html) - serialization of RPC messages into structured data formats
* [ClientTransport](/api/automorph/spi/ClientTransport.html) - RPC transport protocols clients
* [EndpointTransport](/api/automorph/spi/EndpointTransport.html) - adding RPC support to existing servers
* [ServerTransport](/api/automorph/spi/ServerTransport.html) - RPC transport protocol servers
* [RpcProtocol](/api/automorph/spi/RpcProtocol.html) - specific RPC protocol support


## Limitations

* Remote API methods must not use [type parameters](https://docs.scala-lang.org/tour/polymorphic-methods.html)
* Remote APIs must not contain [overloaded methods][https://en.wikipedia.org/wiki/Function_overloading]
* Remote APIs must not be invoked or exposed from within the [App](https://scala-lang.org/api/3.x/scala/App.html) trait nor any other form of [delayed initialization](https://scala-lang.org/api/3.x/scala/DelayedInit.html)
* Due to Scala 2 type inference constraints it may be necessary to explicitly supplying type parameters when composing plugins


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


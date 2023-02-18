# Overview

**Automorph** is an [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) client and server library for [Scala](https://www.scala-lang.org/) providing an effortless way to invoke and expose remote APIs using [JSON-RPC](https://www.jsonrpc.org/specification) and [Web-RPC](Web-RPC) protocols.


## Goals

* Enable consuming and exposing **remote APIs** transparently **without** needing to create **intermediate layer**
* Preserve user's **freedom** to make **technical decisions** and **access transport protocol** metadata
* Strive for **smooth integration** with other **libraries** and existing **applications**


## Supported standards

### RPC protocols

* [JSON-RPC](https://www.jsonrpc.org/specification) (*Default*)
* [Web-RPC](Web-RPC)

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


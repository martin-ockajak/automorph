
**Automorph** is an [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) client and server library for [Scala](https://www.scala-lang.org/) providing an effortless way to invoke and expose remote APIs using [JSON-RPC](https://www.jsonrpc.org/specification) and [REST-RPC](REST-RPC) protocols.

## Goals

* Enable consuming and exposing **remote APIs** transparently **without** needing to create **intermediate layer**
* Preserve user's **freedom** to make **technical decisions** and **access transport protocol** metadata
* Strive for **smooth integration** with other **libraries** and existing **applications**

## Features

* **Convenient** - Generate RPC client or server layer automatically at compile-time from public methods of API classes.
* **Practical** - Access transport protocol request and response metadata using optional API abstractions.
* **Flexible** - Customize remote API function names and mapping between exceptions and RPC protocol errors.
* **Modular** - Choose plugins to select [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) protocol, [effect](https://en.wikipedia.org/wiki/Effect_system) type, [transport](https://en.wikipedia.org/wiki/Transport_layer) protocol and message [format](https://en.wikipedia.org/wiki/File_format).
* **Discoverable** - Consume or provide API schemas through generated yet adjustable discovery functions.
* **Extensible** - Easily implement custom data type serialization support or additional integration plugins.
* **Manageable** - Leverage extensive error handling and structured logging via [SLF4J](http://www.slf4j.org/) to diagnose issues.
* **Compatible** - Artifacts are available for [Scala 3](https://dotty.epfl.ch/) on [JRE 11+](https://openjdk.java.net/) with support for [Scala 2.13](https://www.scala-lang.org/news/2.13.0) and [Scala 2.12](https://www.scala-lang.org/news/2.12.0/) planned.

## Supported standards

### RPC protocols

* [JSON-RPC](https://www.jsonrpc.org/specification) (*Default*)
* [REST-RPC](REST-RPC)

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
* [STTP](https://sttp.softwaremill.com)
* [Tapir](https://tapir.softwaremill.com)
* [ZIO](https://zio.dev)


# Overview

![automorph](../images/banner.jpg)

**Automorph** is an [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) client and server library for [Scala](https://www.scala-lang.org/) providing an effortless
way to invoke and expose remote APIs using [JSON-RPC](https://www.jsonrpc.org/specification) and [REST-RPC] protocols.

## Goals

* Provide a **definitive RPC solution** for Scala ecosystem
* Strive for **easiest possible integration** with existing applications
* Encourage use of **appropriate technical standards** for system interoperability

## Features

* **Powerful** - client and server API bindings are generated directly from public methods of existing API classes
* **Modular** - various integration plugins can be combined to match required effect type, message format and message transport protocol
* **Clean** - request/response metadata can be manipulated using transport protocol agnostic abstraction
* **Fast** - optimized API binding code is generated during compilation
* **Safe** - generated API bindings are type checked during compilation
* **Flexible** - remote API method names and error/exception mapping are customizable
* **Extensible** - additional integration plugins and arbitrary data type serialization are simple to implement
* **Compatible** - available for [Scala 2.12](https://www.scala-lang.org/news/2.12.0/), [Scala 2.13](https://www.scala-lang.org/news/2.13.0)
  and [Scala 3](https://dotty.epfl.ch/)
* **Boilerplate free** - even advanced use-cases require only a few lines of code
* **Dependency free** - core functionality depends on [SLF4J API](http://www.slf4j.org/) only

## Supported standards

### RPC protocols

* [JSON-RPC](https://www.jsonrpc.org/specification)
* [REST-RPC](https://en.wikipedia.org/wiki/Representational_state_transfer)

### Transport protocols

* [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol)
* [WebSocket](https://en.wikipedia.org/wiki/WebSocket)
* [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol)

### Message formats

* [JSON](https://www.json.org/)
* [MessagePack](https://msgpack.org/)

## Inspired by

* [Scala JSON-RPC](https://github.com/shogowada/scala-json-rpc)
* [Autowire](https://github.com/lihaoyi/autowire)
* [STTP](https://github.com/softwaremill/sttp)

# Overview

![automorph](../images/banner.jpg)

**Automorph** is an [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) client and server library for [Scala](https://www.scala-lang.org/) supporting [JSON-RPC](https://www.jsonrpc.org/specification) and [REST-RPC] protocols.

## Goals

* Enable consuming and exposing **remote APIs** transparently and directly **without another layer**
* Strive for **smooth integration** with other **libraries** and existing **applications**
* Preserve user **freedom** to make suitable **technical decisions**

## Features

* **Convenient** - client-side or server-side remote API bindings are created automatically by analyzing public methods of existing API classes
* **Modular** - [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) protocol, [transport](https://en.wikipedia.org/wiki/Transport_layer) protocol, message [format](https://en.wikipedia.org/wiki/File_format) and [effect](https://en.wikipedia.org/wiki/Effect_system) type can be freely combined by choosing appropriate plugins
* **Clean** - request and response transport protocol metadata can be manipulated via optional API abstractions
* **Safe** - optimized API binding code is generated and type checked during compilation
* **Flexible** - remote API method names and exception to error mapping are customizable
* **Extensible** - additional plugins and arbitrary data type serialization support can be added with minimal effort
* **Compatible** - artifacts available for [Scala 2.12](https://www.scala-lang.org/news/2.12.0/), [Scala 2.13](https://www.scala-lang.org/news/2.13.0)
  and [Scala 3](https://dotty.epfl.ch/)
* **Descriptive** - [OpenAPI](https://github.com/OAI/OpenAPI-Specification) specification in [JSON](https://en.wikipedia.org/wiki/JSON) format can be assembled for remote APIs
* **Boilerplate free** - even advanced use-cases require only a few lines of code
* **Dependency free** - core functionality depends on [SLF4J API](http://www.slf4j.org/) only

## Supported standards

### RPC protocols

* [JSON-RPC](https://www.jsonrpc.org/specification)
* [REST-RPC]

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

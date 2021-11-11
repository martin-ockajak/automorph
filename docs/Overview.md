
![automorph](https://github.com/martin-ockajak/automorph/raw/main/docs/images/banner.jpg)

[![Documentation](https://img.shields.io/badge/Website-Documentation-blueviolet)](https://automorph.org/overview.html)
[![API](https://img.shields.io/badge/Scaladoc-API-blue)](https://automorph.org/api/automorph/index.html)
[![Artifacts](https://img.shields.io/badge/Releases-Artifacts-yellow)](https://mvnrepository.com/artifact/org.automorph/automorph)
[![Build](https://github.com/martin-ockajak/automorph/workflows/Build/badge.svg)](https://github.com/martin-ockajak/automorph/actions/workflows/tests.yml)

* [Quick Start](docs/quickstart)
* [Documentation](https://automorph.org)
* [API Reference](https://automorph.org/api/automorph/index.html)


## Goals

* Enable consuming and exposing **remote APIs** transparently **without** needing to create **intermediate layer**
* Preserve user's **freedom** to make **technical decisions** and **access transport protocol** metadata
* Strive for **smooth integration** with other **libraries** and existing **applications**

## Features

* **Convenient** - Client-side or server-side remote API bindings are created automatically from public methods of existing API classes.
* **Modular** - [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) protocol, [effect](https://en.wikipedia.org/wiki/Effect_system) type, [transport](https://en.wikipedia.org/wiki/Transport_layer) protocol and message [format](https://en.wikipedia.org/wiki/File_format) can be freely combined by choosing appropriate plugins.
* **Clean** - Underlying transport protocol request and response metadata can be accessed using optional API abstractions.
* **Safe** - Optimized API binding code is generated and type checked during compilation.
* **Flexible** - Remote API function names and mapping between exceptions and RPC error codes are customizable.
* **Extensible** - Additional plugins and custom data type serialization support can be implemented with minimal effort.
* **Compatible** - Artifacts are currently available for [Scala 3](https://dotty.epfl.ch/) on [JRE 11+](https://openjdk.java.net/) and planned for [Scala 2.13](https://www.scala-lang.org/news/2.13.0) and [Scala 2.12](https://www.scala-lang.org/news/2.12.0/).
* **Discoverable** - Special API functions provide API descriptions using [OpenRPC](https://spec.open-rpc.org) and [OpenAPI](https://github.com/OAI/OpenAPI-Specification) standards.
* **Dependency free** - Core functionality depends on [SLF4J API](http://www.slf4j.org/) only.
* **Boilerplate free** - Even complex or highly specific use-cases require only a few lines of code.

## Supported standards

### Transport protocols

* [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) (*Default*)
* [WebSocket](https://en.wikipedia.org/wiki/WebSocket)
* [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol)

### Message formats

* [JSON](https://www.json.org/) (*Default*)
* [MessagePack](https://msgpack.org/)

### API descriptions

* [OpenRPC](https://spec.open-rpc.org)
* [OpenAPI](https://github.com/OAI/OpenAPI-Specification)

## Inspired by

* [Scala JSON-RPC](https://github.com/shogowada/scala-json-rpc)
* [Autowire](https://github.com/lihaoyi/autowire)
* [STTP](https://github.com/softwaremill/sttp)

![automorph](https://github.com/martin-ockajak/automorph/raw/main/project/images/banner.jpg)

[![Build](https://github.com/martin-ockajak/automorph/workflows/Build/badge.svg)](https://github.com/martin-ockajak/automorph/actions/workflows/tests.yml)
[![Releases](https://img.shields.io/maven-central/v/io.automorph/automorph-core_2.13.svg)](https://mvnrepository.com/artifact/io.automorph)
[![Scaladoc](https://javadoc-badge.appspot.com/io.automorph/automorph-core_2.13.svg?label=scaladoc)](https://javadoc.io/doc/io.automorph/automorph-core_2.13/latest/automorph/)

**Automorph** is an [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) client and server library for [Scala](https://www.scala-lang.org/) providing an effortless
way to invoke and expose remote APIs using [JSON-RPC](https://www.jsonrpc.org/specification) and [REST-RPC](https://en.wikipedia.org/wiki/Representational_state_transfer) protocols.

# [Documentation](https://martin-ockajak.github.io/automorph)

# Overview

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

*Automorph* supports integration with various libraries via plugins published in different artifacts.

## Goals

* Provide a **definitive RPC solution** for Scala ecosystem
* Strive for **easiest possible integration** with existing applications
* Encourage use of **appropriate technical standards** for system interoperability

## Standards

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

## Inspiration

* [Scala JSON-RPC](https://github.com/shogowada/scala-json-rpc)
* [Autowire](https://github.com/lihaoyi/autowire)
* [STTP](https://github.com/softwaremill/sttp)

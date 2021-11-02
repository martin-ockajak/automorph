# Important notice

**This is a preview of an upcoming release. Please do not use it for any purposes other than design review !**

![automorph](https://github.com/martin-ockajak/automorph/raw/main/docs/images/banner.jpg)

[![Build](https://github.com/martin-ockajak/automorph/workflows/Build/badge.svg)](https://github.com/martin-ockajak/automorph/actions/workflows/tests.yml)
[![Releases](https://img.shields.io/maven-central/v/org.automorph/automorph-core_3.0.0.svg)](https://mvnrepository.com/artifact/org.automorph)
[![Scaladoc](https://javadoc-badge.appspot.com/org.automorph/automorph-core_3.0.0.svg?label=scaladoc)](https://javadoc.io/doc/org.automorph/automorph-core_3.0.0/latest/automorph/)

**Automorph** is an [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) client and server library for [Scala](https://www.scala-lang.org/) providing an effortless
way to invoke and expose remote APIs using [JSON-RPC](https://www.jsonrpc.org/specification) and [REST-RPC](docs/rest-rpc/README.md) protocols.

# [Documentation](https://automorph.org)

# Overview

## Goals

* Enable consuming and exposing **remote APIs** transparently **without** needing to create **intermediate layer**
* Preserve user's **freedom** to make **technical decisions** and **access transport protocol** metadata
* Strive for **smooth integration** with other **libraries** and existing **applications**

## Features

* **Convenient** - client-side or server-side remote API bindings are created automatically by analyzing public methods of existing API classes
* **Modular** - [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) protocol, [effect](https://en.wikipedia.org/wiki/Effect_system) type, [transport](https://en.wikipedia.org/wiki/Transport_layer) protocol and message [format](https://en.wikipedia.org/wiki/File_format) and  can be freely combined by choosing appropriate plugins
* **Clean** - request and response transport protocol metadata can be accessed using optional API abstractions
* **Safe** - optimized API binding code is generated and type checked during compilation
* **Flexible** - remote API method names and exception to error mapping are customizable
* **Extensible** - additional plugins and custom data type serialization support can be added with minimal effort
* **Compatible** - artifacts are currently available for [Scala 3](https://dotty.epfl.ch/) and planned also for [Scala 2.13](https://www.scala-lang.org/news/2.13.0) and [Scala 2.12](https://www.scala-lang.org/news/2.12.0/)
* **Descriptive** - [OpenAPI](https://github.com/OAI/OpenAPI-Specification) specification in [JSON](https://en.wikipedia.org/wiki/JSON) format can be assembled for remote APIs
* **Dependency free** - core functionality depends on [SLF4J API](http://www.slf4j.org/) only
* **Boilerplate free** - even advanced use-cases require only a few lines of code


## Supported standards

### RPC protocols

* [JSON-RPC](https://www.jsonrpc.org/specification)
* [REST-RPC](docs/rest-rpc/README.md)

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

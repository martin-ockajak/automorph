<br>

# Important

**This is a preview of an upcoming release. Please do not use it for any purpose other than design review !**

<br>
<br>
<br>

![automorph](https://github.com/martin-ockajak/automorph/raw/main/docs/images/banner.jpg)

[![Documentation](https://img.shields.io/badge/Website-Documentation-blueviolet)](https://automorph.org)
[![API](https://img.shields.io/badge/Scaladoc-API-blue)](https://automorph.org/api/automorph/index.html)
[![Artifacts](https://img.shields.io/badge/Releases-Artifacts-yellow)](https://mvnrepository.com/artifact/org.automorph/automorph)
[![Build](https://github.com/martin-ockajak/automorph/workflows/Build/badge.svg)](https://github.com/martin-ockajak/automorph/actions/workflows/tests.yml)

# Overview

**Automorph** is an [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) client and server library for [Scala](https://www.scala-lang.org/) providing an effortless way to invoke and expose remote APIs using [JSON-RPC](https://www.jsonrpc.org/specification) and [REST-RPC](docs/rest-rpc/README.md) protocols.

* [Quick Start](docs/Quickstart.md)
* [Documentation](https://automorph.org)
* [API](https://automorph.org/api/automorph/index.html)
* [Artifacts](https://mvnrepository.com/artifact/org.automorph/automorph)

## Goals

* Enable consuming and exposing **remote APIs** transparently **without** needing to create **intermediate layer**
* Preserve user's **freedom** to make **technical decisions** and **access transport protocol** metadata
* Strive for **smooth integration** with other **libraries** and existing **applications**

## Features

* **Convenient** - Generate RPC client or server layer automatically at compile-time from public methods of API classes.
* **Practical** - Access transport protocol request and response metadata using optional API abstractions.
* **Flexible** - Customize remote API function names and mapping between exceptions and RPC protocol errors.
* **Modular** - Choose plugins to select [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) protocol, [effect](https://en.wikipedia.org/wiki/Effect_system) type, [transport](https://en.wikipedia.org/wiki/Transport_layer) protocol and message [format](https://en.wikipedia.org/wiki/File_format).
* **Discoverable** - Provide and consume API schemas via discovery functions [OpenRPC](https://spec.open-rpc.org) and [OpenAPI](https://github.com/OAI/OpenAPI-Specification) standards.
* **Extensible** - Easily implement custom data type serialization support or additional integration plugins.
* **Manageable** - Leverage extensive error handling and structured logging via [SLF4J](http://www.slf4j.org/) to diagnose issues.
* **Compatible** - Artifacts are available for [Scala 3](https://dotty.epfl.ch/) on [JRE 11+](https://openjdk.java.net/) with support for [Scala 2.13](https://www.scala-lang.org/news/2.13.0) and [Scala 2.12](https://www.scala-lang.org/news/2.12.0/) planned.

## Supported standards

### RPC protocols

* [JSON-RPC](https://www.jsonrpc.org/specification) (*Default*)
* [REST-RPC](docs/rest-rpc/README.md)

### Transport protocols

* [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) (*Default*)
* [WebSocket](https://en.wikipedia.org/wiki/WebSocket)
* [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol)

### Message formats

* [JSON](https://www.json.org/) (*Default*)
* [MessagePack](https://msgpack.org/)

### API schemas

* [OpenRPC](https://spec.open-rpc.org)
* [OpenAPI](https://github.com/OAI/OpenAPI-Specification)

## Inspired by

* [Scala JSON-RPC](https://github.com/shogowada/scala-json-rpc)
* [Autowire](https://github.com/lihaoyi/autowire)
* [STTP](https://github.com/softwaremill/sttp)

# Build

## Requirements

* [JDK 11+](https://openjdk.java.net/)
* [SBT](https://www.scala-sbt.org/)
* [NodeJS 16+](https://nodejs.org/) *(Documentation)*
* [Yarn](https://yarnpkg.com/) *(Documentation)*

## Commands

### Build

```bash
sbt '+ build'
```

### Documentation

```bash
sbt '++2.13.7 site'
```

### Local dynamic documentation

```bash
yarn --cwd website
DOCS_PATH=../docs yarn --cwd $website start
```

## Notes

* Unified Scaladoc generation does not work for Scala 3
* uPickle codec compilation takes a long time
* Static documentation build breaks API reference links


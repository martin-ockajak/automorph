<br>

# Important

**This is a preview of an upcoming release. Please do not use it for any purpose other than design or code review.**

<br>
<br>
<br>

![automorph](https://github.com/martin-ockajak/automorph/raw/main/docs/images/banner.jpg)

[![Documentation](https://img.shields.io/badge/Website-Documentation-blueviolet)](https://automorph.org)
[![API](https://img.shields.io/badge/Scaladoc-API-blue)](https://automorph.org/api/automorph/index.html)
[![Artifacts](https://img.shields.io/badge/Releases-Artifacts-yellow)](https://mvnrepository.com/artifact/org.automorph/automorph)
[![Build](https://github.com/martin-ockajak/automorph/workflows/Build/badge.svg)](https://github.com/martin-ockajak/automorph/actions/workflows/tests.yml)


# Overview

**Automorph** is an [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) client and server library for [Scala](https://www.scala-lang.org/) providing an effortless way to invoke and expose remote APIs using [JSON-RPC](https://www.jsonrpc.org/specification) and [Web-RPC](docs/Web-RPC.md) protocols.

* [Quick Start](docs/Quickstart.md)
* [Documentation](https://automorph.org)
* [API](https://automorph.org/api/index.html)
* [Artifacts](https://mvnrepository.com/artifact/org.automorph/automorph)


## Goals

* Enable consuming and exposing **remote APIs** transparently **without** needing to create an **intermediate layer**
* Preserve user's **freedom** to make **technical decisions** and **access transport protocol** metadata
* Strive for **smooth integration** with other **libraries** and existing **applications**


## Features

* **Automatic** - Generate [JSON-RPC](https://www.jsonrpc.org/specification) 2.0 and [Web-RPC](docs/Web-RPC.md) 0.1 client or server at compile-time from public API class methods.
* **Modular** - Choose plugins to select [RPC protocol](docs/Plugins.md#rpc-protocol), [effect type](docs/Plugins.md#effect-system), [transport protocol](docs/Plugins.md#message-transport) and [message format](docs/Plugins.md#message-codec).
* **Flexible** - Customize data type serialization, remote API function names and RPC protocol errors.
* **Permissive** - Access transport protocol metadata (e.g. HTTP headers) using optional API abstractions.
* **Discoverable** - Consume and provide [OpenRPC](https://spec.open-rpc.org) 1.3+ or [OpenAPI](https://github.com/OAI/OpenAPI-Specification) 3.1+ API schemas via generated discovery functions.
* **Extensible** - Easily implement custom data type serialization support or additional integration plugins.
* **Compatible** - Requires [Scala](https://dotty.epfl.ch/) 3.2+ or 2.13+ on [JRE 11+](https://openjdk.java.net/) with support for  planned.

## Supported standards

### RPC protocols

* [JSON-RPC](https://www.jsonrpc.org/specification) (*Default*)
* [REST-RPC](docs/rest-rpc/README.md)

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

# Build

## Requirements

* [JDK 11+](https://openjdk.java.net/)
* [SBT](https://www.scala-sbt.org/)
* [NodeJS 17+](https://nodejs.org/) *(Documentation only)*
* [Yarn 1.22+](https://yarnpkg.com/) *(Documentation only)*


## Commands

### Build

```bash
sbt '+ test'
```

#### Set log level

```bash
export LOG_LEVEL=OFF
```

#### Enable generated code logging

```bash
export LOG_CODE=true
```

#### Enable basic tests only

```bash
export TEST_BASIC=true
```

#### Review test logs

```
target/test.log
```

### Documentation

```bash
sbt site
```

#### Continuous rendering

```bash
sbt serveSite
```

## Notes

* uPickle codec compilation for Scala 2 takes a long time
* Monix effect system is missing from API index
* Documentation build breaks API reference links


# Inspired by

* [Scala JSON-RPC](https://github.com/shogowada/scala-json-rpc)
* [Autowire](https://github.com/lihaoyi/autowire)
* [STTP](https://sttp.softwaremill.com)
* [Tapir](https://tapir.softwaremill.com)
* [ZIO](https://zio.dev)

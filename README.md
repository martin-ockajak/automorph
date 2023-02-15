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

* Enable consuming and exposing **remote APIs** transparently **without** needing to create **intermediate layer**
* Preserve user's **freedom** to make **technical decisions** and **access transport protocol** metadata
* Strive for **smooth integration** with other **libraries** and existing **applications**


## Features

* **Automatic** - Generate RPC client or server layer automatically at compile-time from public methods of API classes.
* **Practical** - Access transport protocol request and response metadata using optional API abstractions.
* **Flexible** - Customize remote API function names and mapping between exceptions and RPC protocol errors.
* **Modular** - Choose plugins to select [RPC protocol](docs/Plugins.md#rpc-protocol), [effect type](docs/Plugins.md#effect-system), [transport protocol](docs/Plugins.md#message-transport) and [message format](docs/Plugins.md#message-codec).
* **Discoverable** - Consume or provide API schemas via generated yet adjustable discovery functions.
* **Extensible** - Easily implement custom data type serialization support or additional integration plugins.
* **Manageable** - Leverage extensive error handling and structured [SLF4J](http://www.slf4j.org/)-based logging to diagnose issues.
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


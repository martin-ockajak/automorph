<br>

![automorph](https://github.com/martin-ockajak/automorph/raw/main/site/static/banner.jpg)

[![Documentation](https://img.shields.io/badge/Website-Documentation-purple)](https://automorph.org)
[![API](https://img.shields.io/badge/Scaladoc-API-blue)](https://automorph.org/api/index.html)
[![Artifacts](https://img.shields.io/badge/Releases-Artifacts-yellow)](
https://mvnrepository.com/artifact/org.automorph/automorph)
[![Build](https://github.com/martin-ockajak/automorph/workflows/Build/badge.svg)](
https://github.com/martin-ockajak/automorph/actions/workflows/tests.yml)

## This is a preview of an upcoming release. Please do not attempt to use it but feel free to review.


# Overview

**Automorph** is an [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) client and server library for [Scala](
https://www.scala-lang.org/) providing an effortless way to invoke and expose remote APIs using [JSON-RPC](
https://www.jsonrpc.org/specification) and [Web-RPC](docs/Web-RPC.md) protocols.

* [Quick Start](docs/Quickstart.md)
* [Documentation](https://automorph.org)
* [API](https://automorph.org/api/index.html)
* [Artifacts](https://mvnrepository.com/artifact/org.automorph/automorph)
* [Contact](mailto:automorph.org@proton.me)


# Build

## Requirements

* [JDK](https://openjdk.java.net/) 11+
* [SBT](https://www.scala-sbt.org/) 1.8+
* [NodeJS](https://nodejs.org/) 19+
* [Yarn](https://yarnpkg.com/) 1.22+


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

#### Generate

```bash
sbt site
```

#### Generate continuously

```bash
sbt startSite
```

#### Serve

```bash
sbt serveSite
```

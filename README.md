<br>

![automorph](https://github.com/martin-ockajak/automorph/raw/main/site/static/banner.jpg)

[![Documentation](https://img.shields.io/badge/Website-Documentation-purple)](https://automorph.org)
[![API](https://img.shields.io/badge/Scaladoc-API-blue)](https://automorph.org/api/index.html)
[![Artifacts](https://img.shields.io/badge/Releases-Artifacts-yellow)](
https://mvnrepository.com/artifact/org.automorph/automorph)
[![Build](https://github.com/martin-ockajak/automorph/workflows/Build/badge.svg)](
https://github.com/martin-ockajak/automorph/actions/workflows/tests.yml)


## This is a preview of an upcoming release without the artifacts being published. Please do not use it in any way but feel free to review the documentation.

**Automorph** is an [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) client and server library for [Scala](
https://www.scala-lang.org/) providing an easy way to invoke and expose remote APIs using [JSON-RPC](
https://www.jsonrpc.org/specification) and [Web-RPC](docs/Web-RPC.md) protocols.

* [Quick Start](docs/Quickstart.md)
* [Documentation](https://automorph.org)
* [API](https://automorph.org/api/index.html)
* [Artifacts](https://mvnrepository.com/artifact/org.automorph/automorph)
* [Contact](mailto:automorph.org@proton.me)


# Build

## Requirements

* [JDK](https://openjdk.java.net/) 11+
* [SBT](https://www.scala-sbt.org/) 1.9+
* [NodeJS](https://nodejs.org/) 19+
* [Yarn](https://yarnpkg.com/) 1.22+

**Note**: uPickle plugin build may take a long time but it works.


## Testing

### Test using basic tests

```bash
sbt '+ test'
```

### Test using simple remote API tests only

```bash
TEST_LEVEL=simple sbt '+ test'
```

### Test using complex remote API tests including all integration tests

```bash
TEST_LEVEL=all sbt '+ test'
```

### Test with specific console log level

```bash
LOG_LEVEL=DEBUG sbt '+ test'
```

### Test with generated code logging

```bash
LOG_CODE=true sbt '+ test'
```

### Review test logs

```
less target/test.log
```


## Documentation

### Generate documentation

```bash
sbt site
```

### Serve documentation

```bash
sbt serveSite
```


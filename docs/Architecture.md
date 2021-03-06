# Architecture

## Components

**Automorph** provides the following building blocks to assemble either standalone RPC clients and servers or integrate with existing systems by freely combining various plugins:

* [Client](https://www.javadoc.io/doc/org.automorph/automorph-core_@SCALADOC_VERSION@/latest/automorph/Client.html)
* [Handler](https://www.javadoc.io/doc/org.automorph/automorph-core_@SCALADOC_VERSION@/latest/automorph/Handler.html)
* [RPC protocol](https://www.javadoc.io/doc/org.automorph/automorph-spi_@SCALADOC_VERSION@/latest/automorph/spi/RpcProtocol.html)
* [Effect system](https://www.javadoc.io/doc/org.automorph/automorph-spi_@SCALADOC_VERSION@/latest/automorph/spi/EffectSystem.html)
* [Message transport](https://www.javadoc.io/doc/org.automorph/automorph-spi_@SCALADOC_VERSION@/latest/automorph/spi/MessageTransport.html)
* [Message codec](https://www.javadoc.io/doc/org.automorph/automorph-spi_@SCALADOC_VERSION@/latest/automorph/spi/MessageCodec.html)

## Diagram

![architecture](images/architecture.jpg)

## Client

The client can be used to transparently perform type-safe remote API calls or send one-way messages.

Remote APIs can be invoked statically using transparent proxy instances automatically derived from specified API traits or dynamically by supplying the required type information on invocation.

**Depends on**

* [RPC protocol](https://www.javadoc.io/doc/org.automorph/automorph-spi_@SCALADOC_VERSION@/latest/automorph/spi/RpcProtocol.html)
* [Client message transport](https://www.javadoc.io/doc/org.automorph/automorph-spi_@SCALADOC_VERSION@/latest/automorph/spi/ClientMessageTransport.html)

**Used by**

* Applications

## Handler

The handler can be used to convert remote API calls or one-way messages into type-safe invocations of API instances.

It provides automatic derivation of remote API RPC bindings for existing API implementations and
processing of incoming RPC requests into API invocations resulting in corresponding RPC responses.

**Depends on**

* [RPC protocol](https://www.javadoc.io/doc/org.automorph/automorph-spi_@SCALADOC_VERSION@/latest/automorph/spi/RpcProtocol.html)
* [Effect system](https://www.javadoc.io/doc/org.automorph/automorph-spi_@SCALADOC_VERSION@/latest/automorph/spi/EffectSystem.html)

**Used by**

* [Server message transport](https://www.javadoc.io/doc/org.automorph/automorph-spi_@SCALADOC_VERSION@/latest/automorph/spi/transport/ServerMessageTransport.html)
* [Endpoint message transport](https://www.javadoc.io/doc/org.automorph/automorph-spi_@SCALADOC_VERSION@/latest/automorph/spi/transport/EndpointMessageTransport.html)
* Applications

## Plugins

### RPC protocol

Remote procedure call [protocol](/api/automorph/spi/RpcProtocol.html) plugin.

The underlying RPC protocol must support remote function invocation.

**Depends on**

* [Message codec](https://www.javadoc.io/doc/org.automorph/automorph-spi_@SCALADOC_VERSION@/latest/automorph/spi/MessageCodec.html)

**Used by**

* [Client](https://www.javadoc.io/doc/org.automorph/automorph-core_@SCALADOC_VERSION@/latest/automorph/Client.html)
* [Handler](https://www.javadoc.io/doc/org.automorph/automorph-core_@SCALADOC_VERSION@/latest/automorph/Handler.html)

### Effect system

Computational [effect system](/api/automorph/spi/EffectSystem.html) plugin.

The underlying runtime must support monadic composition of effectful values.

**Used by**

* [Client message transport](https://www.javadoc.io/doc/org.automorph/automorph-spi_@SCALADOC_VERSION@/latest/automorph/spi/ClientMessageTransport.html)
* [Handler](https://www.javadoc.io/doc/org.automorph/automorph-core_@SCALADOC_VERSION@/latest/automorph/Handler.html)

### Message codec

Structured [message format codec](/api/automorph/spi/MessageCodec.html) plugin.

The underlying data format must support storing arbitrarily nested structures of basic data types.

**Used by**

* [RPC protocol](https://www.javadoc.io/doc/org.automorph/automorph-spi_@SCALADOC_VERSION@/latest/automorph/spi/RpcProtocol.html)

### Message transport

Binary [message transport](/api/automorph/spi/MessageTransport.html) protocol plugin.

The underlying transport protocol must support request/response messaging pattern.

#### Client transport

[Client message transport](/api/automorph/spi/transport/ClientMessageTransport.html) protocol plugin.

Passively sends requests and receives responses to and from a remote endpoint using specific transport protocol.

**Depends on**

* [Effect system](https://www.javadoc.io/doc/org.automorph/automorph-spi_@SCALADOC_VERSION@/latest/automorph/spi/EffectSystem.html)

**Used by**

* [Client message transport](https://www.javadoc.io/doc/org.automorph/automorph-spi_@SCALADOC_VERSION@/latest/automorph/spi/ClientMessageTransport.html)

#### Server transport

[Server message transport](/api/automorph/spi/transport/ServerMessageTransport.html) protocol plugin.

Actively receives requests to be processed by the RPC handler and sends responses using specific transport protocol.

**Depends on**

* [Handler](https://www.javadoc.io/doc/org.automorph/automorph-core_@SCALADOC_VERSION@/latest/automorph/Handler.html)

**Used by**

* Applications

#### Endpoint transport

[Endpoint message transport](/api/automorph/spi/transport/EndpointMessageTransport.html) protocol plugin.

Passively parses requests to be processed by the RPC handler and creates responses for specific transport protocol.

**Depends on**

* [Handler](https://www.javadoc.io/doc/org.automorph/automorph-core_@SCALADOC_VERSION@/latest/automorph/Handler.html)

**Used by**

* Applications

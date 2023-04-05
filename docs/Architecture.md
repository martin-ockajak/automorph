---
sidebar_position: 3
---

# Architecture

## Components

**Automorph** provides the following building blocks to assemble either standalone RPC clients and servers or integrate
with existing systems by freely combining its primary componvarious plugins:

* [RPC client](/api/automorph/RpcClient.html)
* [RPC server](/api/automorph/RpcServer.html)
* [RPC endpoint](/api/automorph/RpcEndpoint.html)
* [RPC protocol](/api/automorph/spi/RpcProtocol.html)
* [Effect system](/api/automorph/spi/EffectSystem.html)
* [Message codec](/api/automorph/spi/MessageCodec.html)
* [Client transport](/api/automorph/spi/ClientTransport.html)
* [Server transport](/api/automorph/spi/ServerTransport.html)
* [Endpoint transport](/api/automorph/spi/EndpointTransport.html)


### RPC client & server composition

![RPC client & server](images/architecture-server.jpg)

### RPC client & endpoint composition

![RPC client & endpoint](images/architecture-endpoint.jpg)


## RPC client

Used to perform type-safe remote API calls or send one-way messages.

Remote APIs can be invoked statically using transparent proxy instances automatically derived from specified API
 traits or dynamically by supplying the required type information on invocation.

**Depends on**

* [Client message transport](/api/automorph/spi/ClientMessageTransport.html)
* [RPC protocol](/api/automorph/spi/RpcProtocol.html)

**Used by**

* Applications


## RPC server

Used to serve remote API requests using specific message transport protocol and invoke bound API
methods to process them.

Automatically derives remote API bindings for existing API instances.

**Depends on**

* [Server message transport](/api/automorph/spi/ServerMessageTransport.html)
* [RPC protocol](/api/automorph/spi/RpcProtocol.html)

**Used by**

* Applications


## RPC endpoint

Integrates with an existing message transport layer to receive remote API requests using
specific message transport protocol and invoke bound API methods to process them.

Automatically derives remote API bindings for existing API instances.

**Depends on**

* [Endpoint message transport](/api/automorph/spi/EndpointMessageTransport.html)
* [RPC protocol](/api/automorph/spi/RpcProtocol.html)

**Used by**

* Applications


## [RPC protocol](/api/automorph/spi/RpcProtocol.html)

Remote procedure call protocol plugin.

The underlying RPC protocol must support remote function invocation.

**Depends on**

* [Message codec](/api/automorph/spi/MessageCodec.html)

**Used by**

* [Client](/api/automorph/RpcClient.html)
* [Server](/api/automorph/RpcServer.html)
* [Endpoint](/api/automorph/RpcEndpoint.html)


## [Effect system](/api/automorph/spi/EffectSystem.html)

Computational effect system plugin.

The underlying runtime must support monadic composition of effectful values.

**Used by**

* [Client transport](/api/automorph/spi/ClientTransport.html)
* [Server transport](/api/automorph/spi/ServerTransport.html)
* [Endpoint transport](/api/automorph/spi/EndpointTransport.html)

### [Message codec](/api/automorph/spi/MessageCodec.html)

Structured message format codec plugin.

The underlying data format must support storing arbitrarily nested structures of basic data types.

**Used by**

* [RPC protocol](/api/automorph/spi/RpcProtocol.html)

The underlying transport protocol must support request/response messaging pattern.


## [Client transport](/api/automorph/spi/transport/ClientTransport.html)

Client message transport protocol plugin.

Passively sends requests and receives responses to and from a remote endpoint using specific transport protocol.

**Depends on**

* [Effect system](/api/automorph/spi/EffectSystem.html)

**Used by**

* [Client](/api/automorph/RpcClientMessageTransport.html)


## [Server transport](/api/automorph/spi/transport/ServerTransport.html)

Server message transport protocol plugin.

Actively receives requests to be processed by the RPC handler and sends responses using specific transport protocol.

**Depends on**

* [Effect system](/api/automorph/spi/EffectSystem.html)

**Used by**

* Applications


## [Endpoint transport](/api/automorph/spi/transport/EndpointTransport.html)

Existing server layer integration plugin.

Passively parses requests to be processed by the RPC handler and creates responses for specific transport protocol.

**Depends on**

* [Effect system](/api/automorph/spi/EffectSystem.html)

**Used by**

* Applications

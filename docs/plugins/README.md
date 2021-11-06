# Plugins

*Automorph* supports integration with other software using various plugins of different categories published in separate artifacts.

## Defaults

*Automorph* defines default but easily adjustable choices for various technical concerns aiming at a good balance of capability, performance and simplicity:

* Asynchronous effect: [Future](https://www.scala-lang.org/api/current/scala/concurrent/Future.html)
* Synchronous effect: [Identity](https://www.scala-lang.org/)
* Message format: [JSON](https://www.json.org/)
* Message codec: [Circe](https://circe.github.io/circe)
* Transport protocol: [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol)
* HTTP & WebSocket client: [JRE HTTP client](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html)
* HTTP & WebSocket server: [Undertow](https://undertow.io/)

Special [automorph-default](https://mvnrepository.com/artifact/org.automorph/automorph-default) artifact depends on a set of plugins implementing the default choices. It also provides a convenient way to create default plugin instances using [Default](../api/automorph/Default.html) object.

## RPC protocol

Remote procedure call [protocol](../api/automorph/spi/RpcProtocol.html) plugins.

The underlying RPC protocol must support remote function invocation.

| Class | Artifact | Protocol | Service discovery |
| --- | --- | --- | --- |
| [JsonRpcProtocol](../api/automorph/protocol/JsonRpcProtocol.html) (*Default*) | [automorph-jsonrpc](https://mvnrepository.com/artifact/org.automorph/automorph-jsonrpc) | [JSON-RPC](https://www.jsonrpc.org/specification) | [OpenRPC](https://spec.open-rpc.org), [OpenAPI](https://github.com/OAI/OpenAPI-Specification) |
| [RestRpcProtocol](../api/automorph/protocol/RestRpcProtocol.html) | [automorph-restrpc](https://mvnrepository.com/artifact/org.automorph/automorph-restrpc) | [REST-RPC] | [OpenAPI](https://github.com/OAI/OpenAPI-Specification) |

## Effect system

Computational [effect system](../api/automorph/spi/EffectSystem.html) plugins.

The underlying runtime must support monadic composition of effectful values.

| Class | Artifact | Library | Effect type |
| --- | --- | --- | --- |
| [FutureSystem](../api/automorph/system/FutureSystem.html) (*Default*) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://docs.scala-lang.org/overviews/core/futures.html) | [Future](https://www.scala-lang.org/api/current/scala/concurrent/Future.html) |
| [IdentitySystem](../api/automorph/system/IdentitySystem.html) (Default) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://www.scala-lang.org/) | [Identity](https://www.javadoc.io/doc/org.automorph/automorph-standard_3.0.0/latest/automorph/system/IdentitySystem$$Identity.html) |
| [TrySystem](../api/automorph/system/TrySystem.html) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html) | [Try](https://www.scala-lang.org/api/3.0.0.6/scala/util/Try.html) |
| [ZioSystem](../api/automorph/system/ZioSystem.html) | [automorph-zio](https://mvnrepository.com/artifact/org.automorph/automorph-zio) | [ZIO](https://zio.dev/) | [RIO](https://javadoc.io/doc/dev.zio/zio_3.0.0/latest/zio/RIO$.html) |
| [MonixSystem](../api/automorph/system/MonixSystem.html) | [automorph-monix](https://mvnrepository.com/artifact/org.automorph/automorph-monix) | [Monix](https://monix.io/) | [Task](https://monix.io/api/current/monix/eval/Task.html) |
| [CatsEffectSystem](https://www.javadoc.io/doc/org.automorph/automorph-cats-effect_3.0.0/latest/automorph/system/CatsEffectSystem.html) | [automorph-cats-effect](https://mvnrepository.com/artifact/org.automorph/automorph-cats-effect) | [Cats Effect](https://typelevel.org/cats-effect/) | [IO](https://typelevel.org/cats-effect/api/3.x/cats/effect/IO.html) |
| [ScalazEffectSystem](../api/scalaz/effect/IO.html) |

## Message transport

Binary [message transport](../api/automorph/spi/MessageTransport.html) protocol plugins.

The underlying transport protocol must support request & response messaging pattern.

### Client

[Client message transport](../api/automorph/spi/ClientMessageTransport.html) protocol plugins.

Used by the RPC client to send requests and receive responses to and from a remote endpoint.

| Class | Artifact | Library | Protocol |
| --- | --- | --- | --- |
| [HttpClient](../api/automorph/transport/http/client/HttpClient.html) (*Default*) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [SttpClient](../api/automorph/transport/http/client/SttpClient.html)| [automorph-sttp](https://mvnrepository.com/artifact/org.automorph/automorph-sttp) | [STTP](https://sttp.softwaremill.com/en/latest/) | |
| -> | | [AsyncHttpClient](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| -> | | [Akka HTTP](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| -> | | [Armeria](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| -> | | [HttpClient](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| -> | | [OkHttp](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [JettyClient](../api/automorph/transport/http/endpoint/JettyClient.html) | [automorph-jetty](https://mvnrepository.com/artifact/org.automorph/automorph-jetty) | [Jetty](https://www.eclipse.org/jetty/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [UrlClient](../api/automorph/transport/http/client/UrlClient.html) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [RabbitMqClient](../api/automorph/transport/amqp/client/RabbitMqClient.html) | [automorph-rabbitmq](https://mvnrepository.com/artifact/org.automorph/automorph-rabbitmq) | [RabbitMQ](https://www.rabbitmq.com/java-client.html) | [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol) |

### Server

[Server message transport](../api/automorph/spi/ServerMessageTransport.html) protocol plugins.

Used to actively receive requests and send responses back using specific message transport protocol while invoking RPC request handler to process them.

| Class | Artifact | Library | Protocol |
| --- | --- | --- | --- |
| [UndertowServer](../api/automorph/transport/http/server/UndertowServer.html) (*Default*) | [automorph-undertow](https://mvnrepository.com/artifact/org.automorph/automorph-undertow) | [Undertow](https://undertow.io/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [NanoServer](../api/automorph/transport/http/server/NanoServer.html) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [JettyServer](../api/automorph/transport/http/server/JettyServer.html) | [automorph-jetty](https://mvnrepository.com/artifact/org.automorph/automorph-jetty) | [Jetty](https://www.eclipse.org/jetty/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [RabbitMqServer](../api/automorph/transport/amqp/server/RabbitMqServer.html) | [automorph-rabbitmq](https://mvnrepository.com/artifact/org.automorph/automorph-rabbitmq) | [RabbitMq](https://www.rabbitmq.com/java-client.html) | [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol) |

### Endpoint

[Endpoint message transport](../api/automorph/spi/EndpointMessageTransport.html) protocol plugins.

Used to passively handle requests into responses using specific message transport protocol from an active server while invoking RPC request handler to process them.

| Class | Artifact | Library | Protocol |
| --- | --- | --- | --- |
| [UndertowHttpEndpoint](../api/automorph/transport/http/endpoint/UndertowHttpEndpoint.html) | [automorph-undertow](https://mvnrepository.com/artifact/org.automorph/automorph-undertow) | [Undertow](https://undertow.io/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [UndertowWebSocketEndpoint](../api/automorph/transport/websocket/endpoint/UndertowWebSocketEndpoint.html) | [automorph-undertow](https://mvnrepository.com/artifact/org.automorph/automorph-undertow) | [Undertow](https://undertow.io/) | [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [TapirHttpEndpoint](../api/automorph/transport/http/endpoint/TapirHttpEndpoint.html) | [automorph-tapir](https://mvnrepository.com/artifact/org.automorph/automorph-tapir) | [Tapir](https://tapir.softwaremill.com/) |  |
| -> | | [http4s](https://tapir.softwaremill.com/en/latest/server/http4s.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| -> | | [Akka HTTP](https://tapir.softwaremill.com/en/latest/server/akkahttp.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| -> | | [Finatra](https://tapir.softwaremill.com/en/latest/server/finatra.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| -> | | [Netty](https://tapir.softwaremill.com/en/latest/server/netty.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| -> | | [Play](https://tapir.softwaremill.com/en/latest/server/play.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| -> | | [Vert.X](https://tapir.softwaremill.com/en/latest/server/vertx.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| -> | | [ZIO Http](https://tapir.softwaremill.com/en/latest/server/ziohttp.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [TapirWebSocketEndpoint](../api/automorph/transport/http/endpoint/TapirWebSocketEndpoint.html) | [automorph-tapir](https://mvnrepository.com/artifact/org.automorph/automorph-tapir) | [Tapir](https://tapir.softwaremill.com/) | [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [FinagleEndpoint](../api/automorph/transport/http/endpoint/FinagleEndpoint.html) | [automorph-finagle](https://mvnrepository.com/artifact/org.automorph/automorph-finagle) | [Finagle](https://twitter.github.io/finagle/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [JettyHttpEndpoint](../api/automorph/transport/http/endpoint/JettyHttpEndpoint.html) | [automorph-jetty](https://mvnrepository.com/artifact/org.automorph/automorph-jetty) | [Jetty](https://www.eclipse.org/jetty/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |

## Message codec

Structured [message format codec](../api/automorph/spi/MessageCodec.html) plugins.

The underlying format must support storing arbitrarily nested structures of basic data types.

| Class | Artifact | Library | Node Type | Codec |
| --- | --- | --- | --- | --- |
| [CirceJsonCodec](../api/automorph/format/json/CirceJsonCodec.html) (*Default*) | [automorph-circe](https://mvnrepository.com/artifact/org.automorph/automorph-circe) | [Circe](https://circe.github.io/circe) |[Json](https://circe.github.io/circe/api/io/circe/Json.html) | [JSON](https://www.json.org/) |
| [JacksonJsonCodec](../api/automorph/format/json/JacksonJsonCodec.html) | [automorph-jackson](https://mvnrepository.com/artifact/org.automorph/automorph-jackson) | [Jackson](https://github.com/FasterXML/jackson-module-scala/) |[JsonNode](https://fasterxml.github.io/jackson-databind/javadoc/3.0.0/index.html?com/fasterxml/jackson/databind/JsonNode.html) | [JSON](https://www.json.org/) |
| [UpickleJsonCodec](../api/automorph/format/json/UpickleJsonCodec.html) | [automorph-upickle](https://mvnrepository.com/artifact/org.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Value](http://com-lihaoyi.github.io/upickle/#uJson) | [JSON](https://www.json.org/) |
| [UpickleMessagePackCodec](../api/automorph/format/messagepack/UpickleMessagePackCodec.html) | [automorph-upickle](https://mvnrepository.com/artifact/org.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Msg](https://com-lihaoyi.github.io/upickle/#uPack) | [MessagePack](https://msgpack.org/) |
| [ArgonautJsonCodec](../api/automorph/format/json/ArgonautJsonCodec.html) | [automorph-argonaut](https://mvnrepository.com/artifact/org.automorph/automorph-argonaut) | [Argonaut](http://argonaut.io/doc/) |[Json](http://argonaut.io/scaladocs/#argonaut.Json) | [JSON](https://www.json.org/) |

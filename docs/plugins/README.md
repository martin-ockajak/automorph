# Plugins

*Automorph* supports integration with other software using various plugins published in separate artifacts.

## RPC protocol

Remote procedure call [(RPC) protocol](https://www.javadoc.io/doc/org.automorph/automorph-spi_3.0.0/latest/automorph/spi/RpcProtocol.html) plugins.

The underlying RPC protocol must support remote function invocation.

| Class | Artifact |  Protocol |
| --- | --- | --- |
| [JsonRpcProtocol](https://www.javadoc.io/doc/org.automorph/automorph-standard_3.0.0/latest/automorph/protocol/JsonRpcProtocol.html) (Default) | [automorph-jsonrpc](https://mvnrepository.com/artifact/org.automorph/automorph-jsonrpc) | [JSON-RPC](https://www.jsonrpc.org/specification) |
| [RestRpcProtocol](https://www.javadoc.io/doc/org.automorph/automorph-standard_3.0.0/latest/automorph/protocol/RestRpcProtocol.html) | [automorph-restrpc](https://mvnrepository.com/artifact/org.automorph/automorph-restrpc) | [REST-RPC] |

## Effect system

Computational [effect system](https://www.javadoc.io/doc/org.automorph/automorph-spi_3.0.0/latest/automorph/spi/EffectSystem.html) plugins.

The underlying runtime must support monadic composition of effectful values.

| Class | Artifact | Library | Effect |
| --- | --- | --- | --- |
| [FutureSystem](https://www.javadoc.io/doc/org.automorph/automorph-standard_3.0.0/latest/automorph/system/FutureSystem.html) (Default) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://docs.scala-lang.org/overviews/core/futures.html) | [Future](https://www.scala-lang.org/api/current/scala/concurrent/Future.html) |
| [IdentitySystem](https://www.javadoc.io/doc/org.automorph/automorph-standard_3.0.0/latest/automorph/system/IdentitySystem.html) (Default) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://www.scala-lang.org/) | [Identity](https://www.javadoc.io/doc/org.automorph/automorph-standard_3.0.0/latest/automorph/system/IdentitySystem$$Identity.html) |
| [TrySystem](https://www.javadoc.io/doc/org.automorph/automorph-standard_3.0.0/latest/automorph/system/TrySystem.html) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html) | [Try](https://www.scala-lang.org/api/3.0.0.6/scala/util/Try.html) |
| [ZioSystem](https://www.javadoc.io/doc/org.automorph/automorph-zio_3.0.0/latest/automorph/system/ZioSystem.html) | [automorph-zio](https://mvnrepository.com/artifact/org.automorph/automorph-zio) | [ZIO](https://zio.dev/) | [RIO](https://javadoc.io/doc/dev.zio/zio_3.0.0/latest/zio/RIO$.html) |
| [MonixSystem](https://www.javadoc.io/doc/org.automorph/automorph-monix_3.0.0/latest/automorph/system/MonixSystem.html) | [automorph-monix](https://mvnrepository.com/artifact/org.automorph/automorph-monix) | [Monix](https://monix.io/) | [Task](https://monix.io/api/current/monix/eval/Task.html) |
| [CatsEffectSystem](https://www.javadoc.io/doc/org.automorph/automorph-cats-effect_3.0.0/latest/automorph/system/CatsEffectSystem.html) | [automorph-cats-effect](https://mvnrepository.com/artifact/org.automorph/automorph-cats-effect) | [Cats Effect](https://typelevel.org/cats-effect/) | [IO](https://typelevel.org/cats-effect/api/3.x/cats/effect/IO.html) |
| [ScalazSystem](https://www.javadoc.io/doc/org.automorph/automorph-scalaz_3.0.0/latest/automorph/system/ScalazSystem.html) | [automorph-scalaz](https://mvnrepository.com/artifact/org.automorph/automorph-scalaz) | [Scalaz](https://github.com/scalaz) | [IO](https://www.javadoc.io/doc/org.scalaz/scalaz_3.0.0/latest/scalaz/effect/IO.html) |

## Message transport

Binary [message transport](https://www.javadoc.io/doc/org.automorph/automorph-spi_3.0.0/latest/automorph/spi/MessageTransport.html) protocol plugins.

The underlying transport protocol must support request & response messaging pattern.

### Client

[Client message transport](https://www.javadoc.io/doc/org.automorph/automorph-spi_3.0.0/latest/automorph/spi/ClientMessageTransport.html) protocol plugins.

Used by the RPC client to send requests and receive responses to and from a remote endpoint.

| Class | Artifact | Library | Protocol |
| --- | --- | --- | --- |
| [HttpClient](https://www.javadoc.io/doc/org.automorph/automorph-standard_3.0.0/latest/automorph/transport/http/client/HttpClient.html) - Java 11+ (Default) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [SttpClient](https://www.javadoc.io/doc/org.automorph/automorph-sttp_3.0.0/latest/automorph/transport/http/client/SttpClient.html) | [automorph-sttp](https://mvnrepository.com/artifact/org.automorph/automorph-sttp) | [STTP](https://sttp.softwaremill.com/en/latest/)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| | -> | [Akka HTTP](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| | -> | [Armeria](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
|  | -> | [HttpClient](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
|  | -> | [OkHttp](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
|  | -> | [AsyncHttpClient](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [UrlClient](https://www.javadoc.io/doc/org.automorph/automorph-standard_3.0.0/latest/automorph/transport/http/client/UrlClient.html) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [RabbitMqClient](https://www.javadoc.io/doc/org.automorph/automorph-rabbitmq_3.0.0/latest/automorph/transport/amqp/client/RabbitMqClient.html) | [automorph-rabbitmq](https://mvnrepository.com/artifact/org.automorph/automorph-rabbitmq) | [RabbitMQ](https://www.rabbitmq.com/java-client.html) | [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol) |

### Server

[Server message transport](https://www.javadoc.io/doc/org.automorph/automorph-spi_3.0.0/latest/automorph/spi/ServerMessageTransport.html) protocol plugins.

Used to actively receive requests and send responses back using specific message transport protocol while invoking RPC request handler to process them.

| Class | Artifact | Library | Protocol |
| --- | --- | --- | --- |
| [UndertowServer](https://www.javadoc.io/doc/org.automorph/automorph-undertow_3.0.0/latest/automorph/transport/http/server/UndertowServer.html) (Default) | [automorph-undertow](https://mvnrepository.com/artifact/org.automorph/automorph-undertow) | [Undertow](https://undertow.io/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [NanoServer](https://www.javadoc.io/doc/org.automorph/automorph-standard_3.0.0/latest/automorph/transport/http/server/NanoServer.html) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [RabbitMqServer](https://www.javadoc.io/doc/org.automorph/automorph-rabbitmq_3.0.0/latest/automorph/transport/amqp/server/RabbitMqServer.html) | [automorph-rabbitmq](https://mvnrepository.com/artifact/org.automorph/automorph-rabbitmq) | [RabbitMq](https://www.rabbitmq.com/java-client.html) | [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol) |

### Endpoint

[Endpoint message transport](https://www.javadoc.io/doc/org.automorph/automorph-spi_3.0.0/latest/automorph/spi/EndpointMessageTransport.html) protocol plugins.

Used to passively handle requests into responses using specific message transport protocol from an active server while invoking RPC request handler to process them.

| Class | Artifact | Library | Protocol |
| --- | --- | --- | --- |
| [UndertowHttpEndpoint](https://www.javadoc.io/doc/org.automorph/automorph-undertow_3.0.0/latest/automorph/transport/http/endpoint/UndertowHttpEndpoint.html) | [automorph-undertow](https://mvnrepository.com/artifact/org.automorph/automorph-undertow) | [Undertow](https://undertow.io/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [UndertowWebSocketEndpoint](https://www.javadoc.io/doc/org.automorph/automorph-undertow_3.0.0/latest/automorph/transport/websocket/endpoint/UndertowWebSocketEndpoint.html) | [automorph-undertow](https://mvnrepository.com/artifact/org.automorph/automorph-undertow) | [Undertow](https://undertow.io/) | [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [TapirHttpEndpoint](https://www.javadoc.io/doc/org.automorph/automorph-tapir_3.0.0/latest/automorph/transport/http/endpoint/TapirHttpEndpoint.html) | [automorph-tapir](https://mvnrepository.com/artifact/org.automorph/automorph-tapir) | [Tapir](https://tapir.softwaremill.com/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
|  | -> | [Akka HTTP](https://tapir.softwaremill.com/en/latest/server/akkahttp.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
|  | -> | [Finatra](https://tapir.softwaremill.com/en/latest/server/finatra.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
|  | -> |  [http4s](https://tapir.softwaremill.com/en/latest/server/http4s.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
|  | -> | [Netty](https://tapir.softwaremill.com/en/latest/server/netty.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
|  | -> | [Play](https://tapir.softwaremill.com/en/latest/server/play.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
|  | -> | [Vert.X](https://tapir.softwaremill.com/en/latest/server/vertx.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
|  | -> | [ZIO Http](https://tapir.softwaremill.com/en/latest/server/ziohttp.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [TapirWebSocketEndpoint](https://www.javadoc.io/doc/org.automorph/automorph-tapir_3.0.0/latest/automorph/transport/http/endpoint/TapirWebSocketEndpoint.html) | [automorph-tapir](https://mvnrepository.com/artifact/org.automorph/automorph-tapir) | [Tapir](https://tapir.softwaremill.com/) | [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [FinagleEndpoint](https://www.javadoc.io/doc/org.automorph/automorph-finagle_3.0.0/latest/automorph/transport/http/endpoint/FinagleEndpoint.html) | [automorph-finagle](https://mvnrepository.com/artifact/org.automorph/automorph-finagle) | [Finagle](https://twitter.github.io/finagle/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [JettyEndpoint](https://www.javadoc.io/doc/org.automorph/automorph-jetty_3.0.0/latest/automorph/transport/http/endpoint/JettyEndpoint.html) | [automorph-jetty](https://mvnrepository.com/artifact/org.automorph/automorph-jetty) | [Jetty](https://www.eclipse.org/jetty/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |

## Message codec

Structured [message format codec](https://www.javadoc.io/doc/org.automorph/automorph-spi_3.0.0/latest/automorph/spi/MessageCodec.html) plugins.

The underlying format must support storing arbitrarily nested structures of basic data types.

| Class | Artifact | Library | Node Type | Codec |
| --- | --- | --- | --- | --- |
| [CirceJsonCodec](https://www.javadoc.io/doc/org.automorph/automorph-circe_3.0.0/latest/automorph/format/json/CirceJsonCodec.html) (Default) | [automorph-circe](https://mvnrepository.com/artifact/org.automorph/automorph-circe) | [Circe](https://circe.github.io/circe) |[Json](https://circe.github.io/circe/api/io/circe/Json.html) | [JSON](https://www.json.org/) |
| [JacksonJsonCodec](https://www.javadoc.io/doc/org.automorph/automorph-jackson_3.0.0/latest/automorph/format/json/JacksonJsonCodec.html) | [automorph-jackson](https://mvnrepository.com/artifact/org.automorph/automorph-jackson) | [Jackson](https://github.com/FasterXML/jackson-module-scala/) |[JsonNode](https://fasterxml.github.io/jackson-databind/javadoc/3.0.0/index.html?com/fasterxml/jackson/databind/JsonNode.html) | [JSON](https://www.json.org/) |
| [UpickleJsonCodec](https://www.javadoc.io/doc/org.automorph/automorph-upickle_3.0.0/latest/automorph/format/json/UpickleJsonCodec.html) | [automorph-upickle](https://mvnrepository.com/artifact/org.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Value](http://com-lihaoyi.github.io/upickle/#uJson) | [JSON](https://www.json.org/) |
| [UpickleMessagePackCodec](https://www.javadoc.io/doc/org.automorph/automorph-upickle_3.0.0/latest/automorph/format/messagepack/UpickleMessagePackCodec.html) | [automorph-upickle](https://mvnrepository.com/artifact/org.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Msg](https://com-lihaoyi.github.io/upickle/#uPack) | [MessagePack](https://msgpack.org/) |
| [ArgonautJsonCodec](https://www.javadoc.io/doc/org.automorph/automorph-argonaut_3.0.0/latest/automorph/format/json/ArgonautJsonCodec.html) | [automorph-argonaut](https://mvnrepository.com/artifact/org.automorph/automorph-argonaut) | [Argonaut](http://argonaut.io/doc/) |[Json](http://argonaut.io/scaladocs/#argonaut.Json) | [JSON](https://www.json.org/) |

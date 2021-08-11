# Plugins

*Automorph* supports integration with other software using various plugins published in separate artifacts.

## RPC protocol

Remote procedure call [(RPC) protocol]((https://www.javadoc.io/doc/org.automorph/automorph-spi_2.13/latest/automorph/spi/RpcProtocol.html)) plugins.

The underlying RPC protocol must support remote function invocation.

| Class | Artifact |  Protocol |
| --- | --- | --- |
| [JsonRpcProtocol](https://www.javadoc.io/doc/org.automorph/automorph-standard_2.13/latest/automorph/protocol/JsonRpcProtocol.html) (Default) | [automorph-jsonrpc](https://mvnrepository.com/artifact/org.automorph/automorph-jsonrpc) | [JSON-RPC](https://www.jsonrpc.org/specification) |
| [RestRpcProtocol](https://www.javadoc.io/doc/org.automorph/automorph-standard_2.13/latest/automorph/protocol/RestRpcProtocol.html) | [automorph-restrpc](https://mvnrepository.com/artifact/org.automorph/automorph-restrpc) | [REST-RPC] |

## Effect system

Computational [effect system](https://www.javadoc.io/doc/org.automorph/automorph-spi_2.13/latest/automorph/spi/EffectSystem.html) plugins.

The underlying runtime must support monadic composition of effectful values.

| Class | Artifact | Library | Effect |
| --- | --- | --- | --- |
| [FutureSystem](https://www.javadoc.io/doc/org.automorph/automorph-standard_2.13/latest/automorph/system/FutureSystem.html) (Default) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://docs.scala-lang.org/overviews/core/futures.html) | [Future](https://www.scala-lang.org/api/current/scala/concurrent/Future.html) |
| [IdentitySystem](https://www.javadoc.io/doc/org.automorph/automorph-standard_2.13/latest/automorph/system/IdentitySystem.html) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://www.scala-lang.org/) | [Identity](https://www.javadoc.io/doc/org.automorph/automorph-standard_2.13/latest/automorph/system/IdentitySystem$$Identity.html) |
| [TrySystem](https://www.javadoc.io/doc/org.automorph/automorph-standard_2.13/latest/automorph/system/TrySystem.html) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html) | [Try](https://www.scala-lang.org/api/2.13.6/scala/util/Try.html) |
| [ZioSystem](https://www.javadoc.io/doc/org.automorph/automorph-zio_2.13/latest/automorph/system/ZioSystem.html) | [automorph-zio](https://mvnrepository.com/artifact/org.automorph/automorph-zio) | [ZIO](https://zio.dev/) | [RIO](https://javadoc.io/doc/dev.zio/zio_2.13/latest/zio/RIO$.html) |
| [MonixSystem](https://www.javadoc.io/doc/org.automorph/automorph-monix_2.13/latest/automorph/system/MonixSystem.html) | [automorph-monix](https://mvnrepository.com/artifact/org.automorph/automorph-monix) | [Monix](https://monix.io/) | [Task](https://monix.io/api/current/monix/eval/Task.html) |
| [CatsEffectSystem](https://www.javadoc.io/doc/org.automorph/automorph-cats-effect_2.13/latest/automorph/system/CatsEffectSystem.html) | [automorph-cats-effect](https://mvnrepository.com/artifact/org.automorph/automorph-cats-effect) | [Cats Effect](https://typelevel.org/cats-effect/) | [IO](https://www.javadoc.io/doc/org.typelevel/cats-effect_3/latest/cats/effect/IO.html) |
| [ScalazSystem](https://www.javadoc.io/doc/org.automorph/automorph-scalaz_2.13/latest/automorph/system/ScalazSystem.html) | [automorph-scalaz](https://mvnrepository.com/artifact/org.automorph/automorph-scalaz) | [Scalaz](https://github.com/scalaz) | [IO](https://www.javadoc.io/doc/org.scalaz/scalaz_2.13/latest/scalaz/effect/IO.html) |

## Message transport

Binary [message transport](https://www.javadoc.io/doc/org.automorph/automorph-spi_2.13/latest/automorph/spi/MessageTransport.html) protocol plugins.

The underlying transport protocol must support request/response messaging pattern.

### Client

[Client message transport](https://www.javadoc.io/doc/org.automorph/automorph-spi_2.13/latest/automorph/spi/ClientMessageTransport.html) protocol plugins.

Used by the RPC client to send requests and receive responses to and from a remote endpoint.

| Class | Artifact | Library | Protocol |
| --- | --- | --- | --- |
| [SttpClient](https://www.javadoc.io/doc/org.automorph/automorph-sttp_2.13/latest/automorph/transport/http/client/SttpClient.html) (Default) | [automorph-sttp](https://mvnrepository.com/artifact/org.automorph/automorph-sttp) | [STTP](https://sttp.softwaremill.com/en/latest/)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| | -> | [Akka HTTP](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| | -> | [Armeria](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
|  | -> | [HttpClient](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
|  | -> | [AsyncHttpClient](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
|  | -> | [OkHttp](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [HttpUrlConnectionClient](https://www.javadoc.io/doc/org.automorph/automorph-standard_2.13/latest/automorph/transport/http/client/HttpUrlConnectionClient.html) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [RabbitMqClient](https://www.javadoc.io/doc/org.automorph/automorph-rabbitmq_2.13/latest/automorph/transport/amqp/client/RabbitMqClient.html) | [automorph-rabbitmq](https://mvnrepository.com/artifact/org.automorph/automorph-rabbitmq) | [RabbitMQ](https://www.rabbitmq.com/java-client.html) | [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol) |

### Server

[Server message transport](https://www.javadoc.io/doc/org.automorph/automorph-spi_2.13/latest/automorph/spi/ServerMessageTransport.html) protocol plugins.

Used to actively receive and reply to requests using specific message transport protocol
while invoking RPC request handler to process them.

| Class | Artifact | Library | Protocol |
| --- | --- | --- | --- |
| [UndertowServer](https://www.javadoc.io/doc/org.automorph/automorph-undertow_2.13/latest/automorph/transport/http/server/UndertowServer.html) (Default) | [automorph-undertow](https://mvnrepository.com/artifact/org.automorph/automorph-undertow) | [Undertow](https://undertow.io/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [NanoHttpdServer](https://www.javadoc.io/doc/org.automorph/automorph-standard_2.13/latest/automorph/transport/http/server/NanoHttpdServer.html) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [RabbitMqServer](https://www.javadoc.io/doc/org.automorph/automorph-rabbitmq_2.13/latest/automorph/transport/amqp/server/RabbitMqServer.html) | [automorph-rabbitmq](https://mvnrepository.com/artifact/org.automorph/automorph-rabbitmq) | [RabbitMq](https://www.rabbitmq.com/java-client.html) | [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol) |

### Endpoint

[Endpoint message transport](https://www.javadoc.io/doc/org.automorph/automorph-spi_2.13/latest/automorph/spi/EndpointMessageTransport.html) protocol plugins.

Used to passively receive and reply to requests using specific message transport protocol from an active server while invoking RPC request handler to process them.

| Class | Artifact | Library | Protocol |
| --- | --- | --- | --- |
| [UndertowHttpEndpoint](https://www.javadoc.io/doc/org.automorph/automorph-undertow_2.13/latest/automorph/transport/http/endpoint/UndertowHttpEndpoint.html) | [automorph-undertow](https://mvnrepository.com/artifact/org.automorph/automorph-undertow) | [Undertow](https://undertow.io/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [UndertowWebSocketEndpoint](https://www.javadoc.io/doc/org.automorph/automorph-undertow_2.13/latest/automorph/transport/websocket/endpoint/UndertowWebSocketEndpoint.html) | [automorph-undertow](https://mvnrepository.com/artifact/org.automorph/automorph-undertow) | [Undertow](https://undertow.io/) | [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [TapirHttpEndpoint](https://www.javadoc.io/doc/org.automorph/automorph-tapir_2.13/latest/automorph/transport/http/endpoint/TapirHttpEndpoint.html) | [automorph-tapir](https://mvnrepository.com/artifact/org.automorph/automorph-tapir) | [Tapir](https://tapir.softwaremill.com/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
|  | -> | [Akka HTTP](https://tapir.softwaremill.com/en/latest/server/akkahttp.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
|  | -> | [Finatra](https://tapir.softwaremill.com/en/latest/server/finatra.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
|  | -> |  [http4s](https://tapir.softwaremill.com/en/latest/server/http4s.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
|  | -> | [Play](https://tapir.softwaremill.com/en/latest/server/play.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
|  | -> | [Vert.X](https://tapir.softwaremill.com/en/latest/server/vertx.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
|  | -> | [ZIO Http](https://tapir.softwaremill.com/en/latest/server/ziohttp.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [JettyEndpoint](https://www.javadoc.io/doc/org.automorph/automorph-jetty_2.13/latest/automorph/transport/http/endpoint/JettyEndpoint.html) | [automorph-jetty](https://mvnrepository.com/artifact/org.automorph/automorph-jetty) | [Jetty](https://www.eclipse.org/jetty/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [FinagleEndpoint](https://www.javadoc.io/doc/org.automorph/automorph-finagle_2.13/latest/automorph/transport/http/endpoint/FinagleEndpoint.html) | [automorph-finagle](https://mvnrepository.com/artifact/org.automorph/automorph-finagle) | [Finagle](https://twitter.github.io/finagle/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |

## Message format

Structured [message format](https://www.javadoc.io/doc/org.automorph/automorph-spi_2.13/latest/automorph/spi/MessageFormat.html) serialization/deserialization plugins.

The underlying format must support storing arbitrarily nested structures of basic data types.

| Class | Artifact | Library | Node Type | Format |
| --- | --- | --- | --- | --- |
| [CirceJsonFormat](https://www.javadoc.io/doc/org.automorph/automorph-circe_2.13/latest/automorph/format/json/CirceJsonFormat.html) (Default) | [automorph-circe](https://mvnrepository.com/artifact/org.automorph/automorph-circe) | [Circe](https://circe.github.io/circe) |[Json](https://circe.github.io/circe/api/io/circe/Json.html) | [JSON](https://www.json.org/) |
| [UpickleJsonFormat](https://www.javadoc.io/doc/org.automorph/automorph-upickle_2.13/latest/automorph/format/json/UpickleJsonFormat.html) | [automorph-upickle](https://mvnrepository.com/artifact/org.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Value](http://com-lihaoyi.github.io/upickle/#uJson) | [JSON](https://www.json.org/) |
| [UpickleMessagePackFormat](https://www.javadoc.io/doc/org.automorph/automorph-upickle_2.13/latest/automorph/format/messagepack/UpickleMessagePackFormat.html) | [automorph-upickle](https://mvnrepository.com/artifact/org.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Msg](http://com-lihaoyi.github.io/upickle/#uPack) | [MessagePack](https://msgpack.org/) |
| [ArgonautJsonFormat](https://www.javadoc.io/doc/org.automorph/automorph-argonaut_2.13/latest/automorph/format/json/ArgonautJsonFormat.html) | [automorph-argonaut](https://mvnrepository.com/artifact/org.automorph/automorph-argonaut) | [Argonaut](http://argonaut.io/doc/) |[Json](http://argonaut.io/scaladocs/#argonaut.Json) | [JSON](https://www.json.org/) |


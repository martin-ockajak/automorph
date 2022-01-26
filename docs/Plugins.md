# Plugins

*Automorph* supports integration with other software using various plugins of different categories published in separate artifacts. The plugins are configurable, independently composable and can be instantiated at will.

## Defaults

*Automorph* defines a set of plugins implementing default technical choices aiming at a good balance of features, performance and simplicity:

* Asynchronous effect: [Future](https://www.scala-lang.org/api/current/scala/concurrent/Future.html)
* Synchronous effect: [Identity](https://www.scala-lang.org/api/current/scala/Predef$.html#identity[A](x:A):A)
* Message format: [JSON](https://www.json.org/)
* Message codec: [Circe](https://circe.github.io/circe)
* Transport protocol: [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol)
* HTTP & WebSocket client: [JRE HTTP client](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html)
* HTTP & WebSocket server: [Undertow](https://undertow.io/)

Default plugins set can be obtained by using the [automorph-default](https://mvnrepository.com/artifact/org.automorph/automorph-default) artifact which itself depends on all default plugin artifacts. It also contains the [Default](/api/automorph/Default$.html) object which provides a convenient way to create default plugin instances or compose default plugins with other plugins.

## [RPC protocol](/api/automorph/spi/RpcProtocol.html)

| Class | Artifact | Protocol | Service discovery |
| --- | --- | --- | --- |
| [JsonRpcProtocol](/api/automorph/protocol/JsonRpcProtocol.html) (*Default*) | [automorph-jsonrpc](https://mvnrepository.com/artifact/org.automorph/automorph-jsonrpc) | [JSON-RPC](https://www.jsonrpc.org/specification) | [OpenRPC](https://spec.open-rpc.org), [OpenAPI](https://github.com/OAI/OpenAPI-Specification) |
| [WebRpcProtocol](/api/automorph/protocol/WebRpcProtocol.html) | [automorph-restrpc](https://mvnrepository.com/artifact/org.automorph/automorph-restrpc) | [Web-RPC](Web-RPC) | [OpenAPI](https://github.com/OAI/OpenAPI-Specification) |

## [Effect system](/api/automorph/spi/EffectSystem.html)

| Class | Artifact | Library | Effect type |
| --- | --- | --- | --- |
| [FutureSystem](/api/automorph/system/FutureSystem.html) (*Default*) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://docs.scala-lang.org/overviews/core/futures.html) | [Future](https://www.scala-lang.org/api/current/scala/concurrent/Future.html) |
| [IdentitySystem](/api/automorph/system/IdentitySystem.html) (Default) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://www.scala-lang.org/api/2.13.7/scala/Predef$.html#identity[A](x:A):A) | [Identity](/api/automorph/system/IdentitySystem$.html) |
| [TrySystem](/api/automorph/system/TrySystem.html) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html) | [Try](https://www.scala-lang.org/api/current/scala/util/Try.html) |
| [ZioSystem](/api/automorph/system/ZioSystem.html) | [automorph-zio](https://mvnrepository.com/artifact/org.automorph/automorph-zio) | [ZIO](https://zio.dev/) | [RIO](https://javadoc.io/doc/dev.zio/zio_2.13/latest/zio/RIO$.html) |
| [MonixSystem](/api/automorph/system/MonixSystem.html) | [automorph-monix](https://mvnrepository.com/artifact/org.automorph/automorph-monix) | [Monix](https://monix.io/) | [Task](https://monix.io/api/current/monix/eval/Task.html) |
| [CatsEffectSystem](/api/automorph/system/CatsEffectSystem.html) | [automorph-cats-effect](https://mvnrepository.com/artifact/org.automorph/automorph-cats-effect) | [Cats Effect](https://typelevel.org/cats-effect/) | [IO](https://typelevel.org/cats-effect/api/3.x/cats/effect/IO.html) |
| [ScalazEffectSystem](/api/automorph/system/ScalazEffectSystem.html) | [automorph-scalaz-effect](https://mvnrepository.com/artifact/org.automorph/automorph-scalaz-effect) | [Scalaz Effect](https://github.com/scalaz) | [IO](https://www.javadoc.io/doc/org.scalaz/scalaz_2.13/latest/scalaz/effect/IO.html) |

## [Message codec](/api/automorph/spi/MessageCodec.html)

| Class | Artifact | Library | Node Type | Codec |
| --- | --- | --- | --- | --- |
| [CirceJsonCodec](/api/automorph/codec/json/CirceJsonCodec.html) (*Default*) | [automorph-circe](https://mvnrepository.com/artifact/org.automorph/automorph-circe) | [Circe](https://circe.github.io/circe) |[Json](https://circe.github.io/circe/api/io/circe/Json.html) | [JSON](https://www.json.org/) |
| [JacksonJsonCodec](/api/automorph/codec/json/JacksonJsonCodec.html) | [automorph-jackson](https://mvnrepository.com/artifact/org.automorph/automorph-jackson) | [Jackson](https://github.com/FasterXML/jackson-module-scala/) |[JsonNode](https://fasterxml.github.io/jackson-databind/javadoc/2.13/index.html?com/fasterxml/jackson/databind/JsonNode.html) | [JSON](https://www.json.org/) |
| [UpickleJsonCodec](/api/automorph/codec/json/UpickleJsonCodec.html) | [automorph-upickle](https://mvnrepository.com/artifact/org.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Value](http://com-lihaoyi.github.io/upickle/#uJson) | [JSON](https://www.json.org/) |
| [UpickleMessagePackCodec](/api/automorph/codec/messagepack/UpickleMessagePackCodec.html) | [automorph-upickle](https://mvnrepository.com/artifact/org.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Msg](https://com-lihaoyi.github.io/upickle/#uPack) | [MessagePack](https://msgpack.org/) |
| [ArgonautJsonCodec](/api/automorph/codec/json/ArgonautJsonCodec.html) | [automorph-argonaut](https://mvnrepository.com/artifact/org.automorph/automorph-argonaut) | [Argonaut](http://argonaut.io/doc/) |[Json](http://argonaut.io/scaladocs/#argonaut.Json) | [JSON](https://www.json.org/) |

## [Message transport](/api/automorph/spi/MessageTransport.html)

### [Client transport](/api/automorph/spi/transport/ClientMessageTransport.html)

| Class | Artifact | Library | Protocol |
| --- | --- | --- | --- |
| [HttpClient](/api/automorph/transport/http/client/HttpClient.html) (*Default*) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [SttpClient](/api/automorph/transport/http/client/SttpClient.html)| [automorph-sttp](https://mvnrepository.com/artifact/org.automorph/automorph-sttp) | [STTP](https://sttp.softwaremill.com/en/latest/) | |
| -> | | [AsyncHttpClient](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| -> | | [Akka HTTP](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| -> | | [Armeria](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| -> | | [HttpClient](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| -> | | [OkHttp](https://sttp.softwaremill.com/en/latest/backends/summary.html)| [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [JettyClient](/api/automorph/transport/http/client/JettyClient.html) | [automorph-jetty](https://mvnrepository.com/artifact/org.automorph/automorph-jetty) | [Jetty](https://www.eclipse.org/jetty/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [UrlClient](/api/automorph/transport/http/client/UrlClient.html) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard) | [Standard](https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [RabbitMqClient](/api/automorph/transport/amqp/client/RabbitMqClient.html) | [automorph-rabbitmq](https://mvnrepository.com/artifact/org.automorph/automorph-rabbitmq) | [RabbitMQ](https://www.rabbitmq.com/java-client.html) | [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol) |

### [Server transport](/api/automorph/spi/transport/ServerMessageTransport.html)

| Class | Artifact                                                                                    | Library                                               | Protocol |
| --- |---------------------------------------------------------------------------------------------|-------------------------------------------------------| --- |
| [UndertowServer](/api/automorph/transport/http/server/UndertowServer.html) (*Default*) | [automorph-undertow](https://mvnrepository.com/artifact/org.automorph/automorph-undertow)   | [Undertow](https://undertow.io/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [VertxServer](/api/automorph/transport/http/server/VertxServer.html) | [automorph-vertx](https://mvnrepository.com/artifact/org.automorph/automorph-vertx)         | [Vert.x](https://vertx.io/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [NanoServer](/api/automorph/transport/http/server/NanoServer.html) | [automorph-standard](https://mvnrepository.com/artifact/org.automorph/automorph-standard)   | [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd)   | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [JettyServer](/api/automorph/transport/http/server/JettyServer.html) | [automorph-jetty](https://mvnrepository.com/artifact/org.automorph/automorph-jetty)         | [Jetty](https://www.eclipse.org/jetty/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [AkkaServer](/api/automorph/transport/http/server/AkkaServer.html) | [automorph-akka-http](https://mvnrepository.com/artifact/org.automorph/automorph-akka-http) | [Akka HTTP](https://doc.akka.io/docs/akka-http/current/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [RabbitMqServer](/api/automorph/transport/amqp/server/RabbitMqServer.html) | [automorph-rabbitmq](https://mvnrepository.com/artifact/org.automorph/automorph-rabbitmq)   | [RabbitMq](https://www.rabbitmq.com/java-client.html) | [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol) |

### [Endpoint transport](/api/automorph/spi/transport/EndpointMessageTransport.html)

| Class | Artifact                                                                                    | Library | Protocol |
| --- |---------------------------------------------------------------------------------------------| --- | --- |
| [UndertowHttpEndpoint](/api/automorph/transport/http/endpoint/UndertowHttpEndpoint.html) | [automorph-undertow](https://mvnrepository.com/artifact/org.automorph/automorph-undertow)   | [Undertow](https://undertow.io/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [UndertowWebSocketEndpoint](/api/automorph/transport/websocket/endpoint/UndertowWebSocketEndpoint$.html) | [automorph-undertow](https://mvnrepository.com/artifact/org.automorph/automorph-undertow)   | [Undertow](https://undertow.io/) | [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [VertxHttpEndpoint](/api/automorph/transport/http/endpoint/VertxHttpEndpoint.html) | [automorph-vertx](https://mvnrepository.com/artifact/org.automorph/automorph-vertx)         | [Vert.x](https://vertx.io/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [VertxWebSocketEndpoint](/api/automorph/transport/websocket/endpoint/VertxWebSocketEndpoint$.html) | [automorph-vertx](https://mvnrepository.com/artifact/org.automorph/automorph-vertx)         | [Vert.x](https://vertx.io/) | [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [FinagleEndpoint](/api/automorph/transport/http/endpoint/FinagleHttpEndpoint.html) | [automorph-finagle](https://mvnrepository.com/artifact/org.automorph/automorph-finagle)     | [Finagle](https://twitter.github.io/finagle/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [JettyHttpEndpoint](/api/automorph/transport/http/endpoint/JettyHttpEndpoint.html) | [automorph-jetty](https://mvnrepository.com/artifact/org.automorph/automorph-jetty)         | [Jetty](https://www.eclipse.org/jetty/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [AkkaHttpEndpoint](/api/automorph/transport/http/endpoint/AkkaHttpEndpoint.html) | [automorph-akka-http](https://mvnrepository.com/artifact/org.automorph/automorph-akka-http) | [Akka HTTP](https://doc.akka.io/docs/akka-http/current/) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |

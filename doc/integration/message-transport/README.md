# Message transport

Binary [message transport](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/MessageTransport.html) protocol plugins.

The underlying transport protocol must support request/response messaging pattern.

## Client

[Client message transport](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/ClientMessageTransport.html) protocol plugins.

Used by the RPC client to send requests and receive responses to and from a remote endpoint.

| Class | Artifact | Library | Effect Type | Protocol |
| ---- | --- | --- | --- | --- |
| [SttpClient](https://www.javadoc.io/doc/io.automorph/automorph-sttp_2.13/latest/automorph/transport/http/client/SttpClient.html) (Default) | [automorph-sttp](https://mvnrepository.com/artifact/io.automorph/automorph-sttp) | [STTP](https://sttp.softwaremill.com/en/latest/) -> [Akka HTTP, AsyncHttpClient, HttpClient, OkHttp](https://sttp.softwaremill.com/en/latest/backends/summary.html)| *All* | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [HttpUrlConnectionClient](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/transport/http/client/HttpUrlConnectionClient.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard](https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html) | [Identity](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/system/IdentitySystem$$Identity.html) | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [RabbitMqClient](https://www.javadoc.io/doc/io.automorph/automorph-rabbitmq_2.13/latest/automorph/transport/amqp/client/RabbitMqClient.html) | [automorph-rabbitmq](https://mvnrepository.com/artifact/io.automorph/automorph-rabbitmq) | [RabbitMq](https://www.rabbitmq.com/java-client.html) | [Future](https://www.scala-lang.org/api/current/scala/concurrent/Future.html) | [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol) |

## Server

[Server message transport](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/ServerMessageTransport.html) protocol plugins.

Used to actively receive and reply to requests using specific message transport protocol
while invoking RPC request handler to process them.

| Class | Artifact | Library | Effect Type | Protocol |
| ---- | --- | --- | --- | --- |
| [UndertowServer](https://www.javadoc.io/doc/io.automorph/automorph-undertow_2.13/latest/automorph/transport/http/server/UndertowServer.html) (Default) | [automorph-undertow](https://mvnrepository.com/artifact/io.automorph/automorph-undertow) | [Undertow](https://undertow.io/) | *All* | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [NanoHttpdServer](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/transport/http/server/NanoHttpdServer.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd) | *All* | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [RabbitMqServer](https://www.javadoc.io/doc/io.automorph/automorph-rabbitmq_2.13/latest/automorph/transport/amqp/server/RabbitMqServer.html) | [automorph-rabbitmq](https://mvnrepository.com/artifact/io.automorph/automorph-rabbitmq) | [RabbitMq](https://www.rabbitmq.com/java-client.html) | *All* | [AMQP](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol) |

## Endpoint

[Endpoint message transport](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/EndpointMessageTransport.html) protocol plugins.

Used to passively receive and reply to requests using specific message transport protocol from an active server while invoking RPC request handler to process them.

| Class | Artifact | Library | Effect Type | Protocol |
| ---- | --- | --- | --- | --- |
| [UndertowHttpEndpoint](https://www.javadoc.io/doc/io.automorph/automorph-undertow_2.13/latest/automorph/transport/http/endpoint/UndertowHttpEndpoint.html) | [automorph-undertow](https://mvnrepository.com/artifact/io.automorph/automorph-undertow) | [Undertow](https://undertow.io/) | *All* | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [UndertowWebSocketEndpoint](https://www.javadoc.io/doc/io.automorph/automorph-undertow_2.13/latest/automorph/transport/websocket/endpoint/UndertowWebSocketEndpoint.html) | [automorph-undertow](https://mvnrepository.com/artifact/io.automorph/automorph-undertow) | [Undertow](https://undertow.io/) | *All* | [WebSocket](https://en.wikipedia.org/wiki/WebSocket) |
| [TapirHttpEndpoint](https://www.javadoc.io/doc/io.automorph/automorph-tapir_2.13/latest/automorph/transport/http/endpoint/TapirHttpEndpoint.html) | [automorph-tapir](https://mvnrepository.com/artifact/io.automorph/automorph-tapir) | [Tapir](https://tapir.softwaremill.com/) -> [Akka HTTP](https://tapir.softwaremill.com/en/latest/server/akkahttp.html), [Finatra](https://tapir.softwaremill.com/en/latest/server/finatra.html), [http4s](https://tapir.softwaremill.com/en/latest/server/http4s.html), [Play](https://tapir.softwaremill.com/en/latest/server/play.html), [Vert.X](https://tapir.softwaremill.com/en/latest/server/vertx.html), [ZIO Http](https://tapir.softwaremill.com/en/latest/server/ziohttp.html) | *All* | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [JettyEndpoint](https://www.javadoc.io/doc/io.automorph/automorph-jetty_2.13/latest/automorph/transport/http/endpoint/JettyEndpoint.html) | [automorph-jetty](https://mvnrepository.com/artifact/io.automorph/automorph-jetty) | [Jetty](https://www.eclipse.org/jetty/) | *All* | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |
| [FinagleEndpoint](https://www.javadoc.io/doc/io.automorph/automorph-finagle_2.13/latest/automorph/transport/http/endpoint/FinagleEndpoint.html) | [automorph-finagle](https://mvnrepository.com/artifact/io.automorph/automorph-finagle) | [Finagle](https://twitter.github.io/finagle/) | *All* | [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) |

# Examples

## Synchronous

* [Source](/test/examples/src/test/scala/test/examples/Synchronous.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
import automorph.Default
import java.net.URI

// Define an API type and create its instance
class Api {
  def hello(some: String, n: Int): String =
    s"Hello $some $n!"
}
val api = new Api()
```

**Server**

```scala
// Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
val createServer = Default.serverSync(80, "/api")
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientSync(new URI("http://localhost/api"), "POST")

// Call the remote API function
val remoteApi = client.bind[Api] // Api
remoteApi.hello("world", 1) // String

// Close the client
client.close()
```

## Asynchronous

* [Source](/test/examples/src/test/scala/test/examples/Asynchronous.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Define an API and create its instance
class Api {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new Api()

```

**Server**

```scala
// Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
val createServer = Default.serverAsync(80, "/api")
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.asyncHttpClient(new URI("http://localhost/api"), "POST")

// Call the remote API function
val remoteApi = client.bind[Api] // Api
remoteApi.hello("world", 1) // Future[String]

// Close the client
client.close()
```

## Dynamic notification

* [Source](/test/examples/src/test/scala/test/examples/DynamicNotification.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Define an API and create its instance
class Api {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new Api()

```

**Server**

```scala
// Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
val createServer = Default.serverAsync(80, "/api")
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.asyncHttpClient(new URI("http://localhost/api"), "POST")

// Call the remote API function dynamically
val remoteHello = client.function("hello")
remoteHello.args("some" -> "world", "n" -> 1).call[String] // Future[String]

// Notify the remote API function dynamically without expecting a response
remoteHello.args("some" -> "world", "n" -> 1).tell // Future[Unit]

// Close the client
client.close()
```

## Request metadata

* [Source](/test/examples/src/test/scala/test/examples/RequestMetadata.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
// Define server API type and create its instance
import automorph.Default
import java.net.URI

// Define server API type and create its instance
class ServerApi {

  // Use HTTP request metadata context provided by the server message transport plugin
  def contextual(message: String)(
    implicit requestContext: Default.ServerContext
  ): String = Seq(
    Some(message),
    requestContext.path,
    requestContext.header("X-Test")
  ).flatten.mkString(",")
}
val api = new ServerApi()

// Define client view of the server API
trait ClientApi {

  // Use HTTP request context defined by the client message transport plugin
  def contextual(message: String)(implicit request: Default.ClientContext): String
}
```

**Server**

```scala
// Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
val createServer = Default.serverSync(80, "/api")
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientSync(new URI("http://localhost/api"), "POST")

// Create client request context specifying HTTP request meta-data
val requestContext = client.defaultContext
  .parameters("test" -> "value")
  .headers("X-Test" -> "value", "Cache-Control" -> "no-cache")
  .cookies("Test" -> "value")
  .authorizationBearer("value")

// Call the remote API function statically with request context supplied directly
val remoteApi = client.bind[ClientApi] // Api
remoteApi.contextual("test")(using requestContext) // String

// Call the remote API function statically with request context supplied implictly
implicit val givenRequestMetadata: Default.ClientContext = requestContext
remoteApi.contextual("test") // String

// Call the remote API function dynamically with request context supplied directly
val callContextual = client.call[String]("contextual")
callContextual.args("message" -> "test")(using requestContext) // String

// Call the remote API function dynamically with request context supplied implictly
callContextual.args("message" -> "test") // String

// Close the client
client.close()
```

## Name mapping

* [Source](/test/examples/src/test/scala/test/examples/MethodMapping.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
import automorph.Default
import java.net.URI
import scala.util.Try

// Define an API type and create its instance
class Api {
  // Exposed both as 'original' and 'custom'
  def original(value: Option[String]): String =
    value.getOrElse("")

  // Not exposed
  def omitted(): String =
    ""

  // Exposed as 'test.multi'
  def multi(add: Boolean)(n: Double): Double =
    if (add) n + 1 else n - 1
}

val api = new Api()
```

**Server**

```scala
// Customize RPC function names
val mapNames = (name: String) => name match {
  case "original" => Seq("original", "custom")
  case "omitted" => Seq.empty
  case other => Seq(s"test.$other")
}

// Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
val createServer = Default.serverSync(80, "/api")
val server = createServer(_.bind(api, mapNames(_)))

// Stop the server
server.close()
```

**Client**

```scala
// Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientSync(new URI("http://localhost/api"), "POST")

// Call the remote API function dynamically
client.notify("custom").args("value" -> None) // ()
Try(client.call[String]("omitted").args()) // Failure
client.call[Double]("test.multi").args("add" -> true, "n" -> 1) // 2

// Close the client
client.close()
```

## Error mapping

* [Source](/test/examples/src/test/scala/test/examples/ErrorMapping.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
import automorph.protocol.jsonrpc.ErrorType.InvalidRequest
import automorph.transport.http.HttpContext
import automorph.{Client, Default, Handler}
import java.net.URI
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Define an API type and create its instance
class Api {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}

val api = new Api()

```

**Server**

```scala
// Customize server RPC error mapping
val protocol = Default.protocol
val serverProtocol = protocol.mapException {
  case _: SQLException => InvalidRequest
  case e => protocol.exceptionToError(e)
}

// Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
val system = Default.systemAsync
val handler = Handler
  .protocol(serverProtocol).system(system).context[Default.ServerContext]
val createServer = Default.server(handler, 80, "/api", {
  // Customize server HTTP status code mapping
  case _: SQLException => 400
  case e => HttpContext.defaultExceptionToStatusCode(e)
})
val server = createServer(_ => ())

// Stop the server
server.close()
```

**Client**

```scala
// Customize client RPC error mapping
val clientProtocol = protocol.mapError {
  case (message, InvalidRequest.code) if message.contains("SQL") =>
    new SQLException(message)
  case (message, code) => protocol.errorToException(message, code)
}

// Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val transport = Default.clientAsyncTransport(new URI("http://localhost/api"), "POST")
val client = Client.protocol(clientProtocol).transport(transport)

// Call the remote API function
val remoteApi = client.bind[Api] // Api
remoteApi.hello("world", 1) // Future[String]

// Close the client
client.close()
```

## Effect system

* [Source](/test/examples/src/test/scala/test/examples/EffectSystem.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1",
  "org.automorph" %% "automorph-zio" % "0.0.1",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % "3.3.9"
)
```

**API**

```scala
import automorph.Default
import automorph.system.ZioSystem
import java.net.URI
import org.asynchttpclient.DefaultAsyncHttpClient
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.{Runtime, Task}

// Define an API type and create its instance
class Api {
  def hello(some: String, n: Int): Task[String] =
    Task.succeed(s"Hello $some $n!")
}
val api = new Api()
```

**Server**

```scala
// Create an effect system plugin
val system = ZioSystem[Any]()

// Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
val server = Default.serverSystem(system, 80, "/api")(_.bind(api)) {
  (effect: ZioSystem.Effect[Any]) =>
    Runtime.default.unsafeRunTask(effect)
    ()
}

// Stop the server
server.close()
```

**Client**

```scala
// Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val backend = AsyncHttpClientZioBackend
  .usingClient(Runtime.default, new DefaultAsyncHttpClient())
val client = Default.client(new URI("http://localhost/api"), "POST", backend, system)

// Call the remote API function
val remoteApi = client.bind[Api] // Api
remoteApi.hello("world", 1) // Task[String]

// Close the client
client.close()
```

## RPC protocol

* [Source](/test/examples/src/test/scala/test/examples/RpcProtocol.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
import automorph.protocol.RestRpcProtocol
import automorph.{Client, Default, Handler}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Define an API type and create its instance
class Api {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new Api()

```

**Server**

```scala
// Create REST-RPC protocol plugin
val protocol = RestRpcProtocol[Default.Node, Default.Codec](Default.codec)

// Start Undertow REST-RPC HTTP server listening on port 80 for requests to '/api'
val system = Default.asyncSystem
val handler = Handler.protocol(protocol).system(system).context[Default.ServerContext]
val createServer = Default.server(handler, 80, "/api")
val server = createServer(_ => ())

// Stop the server
server.close()
```

**Client**

```scala
// Setup STTP REST-RPC HTTP client sending POST requests to 'http://localhost/api'
val transport = Default.clientAsyncTransport(new URI("http://localhost/api"), "POST")
val client = Client.protocol(protocol).transport(transport)

// Call the remote API function
val remoteApi = client.bind[Api] // Api
remoteApi.hello("world", 1) // Future[String]

// Close the client
client.close()
```

## Message codec

* [Source](/test/examples/src/test/scala/test/examples/MessageCodec.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1",
  "org.automorph" %% "automorph-upickle" % "0.0.1"
)
```

**API**

```scala
import automorph.codec.messagepack.UpickleMessagePackCodec
import automorph.{Client, Default, Handler}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Define an API type and create its instance
case class Record(values: List[String])
class Api {
  def hello(some: String, n: Int): Future[Record] =
    Future(Record(List("Hello", some, n.toString)))
}
val api = new Api()
```

**Server**

```scala
// Create uPickle message codec for JSON format
val codec = UpickleMessagePackCodec()

// Create custom data type serializer/deserializer
implicit def recordRw: codec.custom.ReadWriter[Record] = codec.custom.macroRW

// Create an RPC protocol plugin
val protocol = Default.protocol[UpickleMessagePackCodec.Node, codec.type](codec)

// Create an effect system plugin
val system = Default.asyncSystem

// Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
val handler = Handler.protocol(protocol).system(system).context[Default.ServerContext]
val createServer = Default.server(handler.bind(api), 80, "/api")
val server = createServer(_ => ())

// Stop the server
server.close()
```

**Client**

```scala
// Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val transport = Default.clientAsyncTransport(new URI("http://localhost/api"), "POST")
val client = Client(protocol, transport)

// Call the remote API function
val remoteApi = client.bind[Api] // Api
remoteApi.hello("world", 1) // Future[String]

// Close the client
client.close()
```

## Client message transport

* [Source](/test/examples/src/test/scala/test/examples/ClientMessageTransport.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
import automorph.Default
import automorph.system.IdentitySystem
import automorph.transport.http.client.UrlClient
import java.net.URI

// Define an API type and create its instance
class Api {
  def hello(some: String, n: Int): String =
    s"Hello $some $n!"
}
val api = new Api()
```

**Server**

```scala
// Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
val createServer = Default.serverSync(80, "/api")
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Create HttpUrlConnection HTTP client message transport
val transport = UrlClient(new URI("http://localhost/api"), "POST", IdentitySystem())

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.client(transport)

// Call the remote API function
val remoteApi = client.bind[Api] // Api
remoteApi.hello("world", 1) // String

// Close the client
client.close()
```

## Server message transport

* [Source](/test/examples/src/test/scala/test/examples/ServerMessageTransport.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
import automorph.Default
import automorph.transport.http.server.NanoServer
import java.net.URI

// Define an API type and create its instance
class Api {
  def hello(some: String, n: Int): String =
    s"Hello $some $n!"
}
val api = new Api()
```

**Server**

```scala
// Start NanoHTTPD JSON-RPC HTTP server listening on port 80 for requests to '/api'
val handler = Default.handlerSync[NanoServer.Context]
val createServer = NanoServer.create(handler.bind(api), 80)
val server = createServer(identity)

// Stop the server
server.close()
```

**Client**

```scala
// Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientSync(new URI("http://localhost/api"), "POST")

// Call the remote API function
val remoteApi = client.bind[Api] // Api
remoteApi.hello("world", 1) // String

// Close the client
client.close()
```

## Endpoint message transport

* [Source](/test/examples/src/test/scala/test/examples/EndpointMessageTransport.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
import automorph.Default
import automorph.transport.http.endpoint.UndertowHttpEndpoint
import io.undertow.{Handlers, Undertow}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Define an API type and create its instance
class Api {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new Api()
```

**Server**

```scala
// Create custom Undertow JSON-RPC endpoint
val handler = Default.handlerAsync[UndertowHttpEndpoint.Context]
val createEndpoint = UndertowHttpEndpoint.create(handler.bind(api))
val endpoint = createEndpoint(_ => ())

// Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
val server = Undertow.builder()
  .addHttpListener(80, "0.0.0.0")
  .setHandler(Handlers.path().addPrefixPath("/api", endpoint))
  .build()

// Stop the server
server.stop()
```

**Client**

```scala
// Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientAsync(new URI("http://localhost/api"), "POST")

// Call the remote API function
val remoteApi = client.bind[Api] // Api
remoteApi.hello("world", 1) // Future[String]

// Close the client
client.close()
```

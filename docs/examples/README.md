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
// Start default JSON-RPC HTTP server listening on port 80 for requests to '/api'
val createServer = Default.serverSync(80, "/api")
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientSync(new URI("http://localhost/api"))

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
// Start default JSON-RPC HTTP server listening on port 80 for requests to '/api'
val createServer = Default.serverAsync(80, "/api")
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.asyncHttpClient(new URI("http://localhost/api"))

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
// Start default JSON-RPC HTTP server listening on port 80 for requests to '/api'
val createServer = Default.serverAsync(80, "/api")
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.asyncHttpClient(new URI("http://localhost/api"))

// Notify the remote API function dynamically without expecting a response
client.notify("hello").args("some" -> "world", "n" -> 1) // Future[Unit]

// Close the client
client.close()
```

## HTTP request metadata

* [Source](/test/examples/src/test/scala/test/examples/HttpRequestMetadata.scala)

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
import automorph.Default.{ClientContext, ServerContext}
import java.net.URI

// Define server API type and create its instance
class ServerApi {

  // Use HTTP request metadata context provided by the server message transport plugin
  def hello(message: String)(implicit requestContext: ServerContext): String = Seq(
    Some(message),
    requestContext.path,
    requestContext.header("X-Test")
  ).flatten.mkString(", ")
}
val api = new ServerApi()

// Define client view of the server API
trait ClientApi {

  // Use HTTP request context defined by the client message transport plugin
  def hello(message: String)(implicit request: ClientContext): String
}
```

**Server**

```scala
// Start default JSON-RPC HTTP server listening on port 80 for requests to '/api'
val createServer = Default.serverSync(80, "/api")
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientSync(new URI("http://localhost/api"))

// Create client request context specifying HTTP request meta-data
val requestContext = client.defaultContext
  .parameters("test" -> "value")
  .headers("X-Test" -> "value", "Cache-Control" -> "no-cache")
  .cookies("Test" -> "value")
  .authorizationBearer("value")

// Call the remote API function statically with request context supplied directly
val remoteApi = client.bind[ClientApi] // Api
remoteApi.hello("test")(using requestContext) // String

// Call the remote API function statically with request context supplied implictly
implicit val givenRequestMetadata: ClientContext = requestContext
remoteApi.hello("test") // String

// Call the remote API function dynamically with request context supplied directly
client.call[String]("hello").args("message" -> "test")(using requestContext) // String

// Call the remote API function dynamically with request context supplied implictly
client.call[String]("hello").args("message" -> "test") // String

// Close the client
client.close()
```

## Function name mapping

* [Source](/test/examples/src/test/scala/test/examples/NameMapping.scala)

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
  // Exposed both as 'hello' and 'custom'
  def hello(value: Option[String]): String =
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
val mapName = (name: String) => name match {
  case "hello" => Seq("hello", "custom")
  case "omitted" => Seq.empty
  case other => Seq(s"test.$other")
}

// Start default JSON-RPC HTTP server listening on port 80 for requests to '/api'
val createServer = Default.serverSync(80, "/api")
val server = createServer(_.bind(api, mapName(_)))

// Stop the server
server.close()
```

**Client**

```scala
// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientSync(new URI("http://localhost/api"))

// Call the remote API function dynamically
client.notify("custom").args("value" -> None) // ()
Try(client.call[String]("omitted").args()) // Failure
client.call[Double]("test.multi").args("add" -> true, "n" -> 1) // 2

// Close the client
client.close()
```

## Client error mapping

* [Source](/test/examples/src/test/scala/test/examples/ClientErrorMapping.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
import automorph.protocol.jsonrpc.ErrorType.InvalidRequest
import automorph.{Client, Default}
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
// Start default JSON-RPC HTTP server listening on port 80 for requests to '/api'
val createServer = Default.serverAsync(80, "/api")
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Customize remote API client RPC error to exception mapping
val protocol = Default.protocol.mapError {
  case (message, InvalidRequest.code) if message.contains("SQL") =>
    new SQLException(message)
  case (message, code) => Default.protocol.errorToException(message, code)
}

// Setup custom JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val transport = Default.clientTransportAsync(new URI("http://localhost/api"))
val client = Client.protocol(protocol).transport(transport)

// Call the remote API function
val remoteApi = client.bind[Api] // Api
remoteApi.hello("world", 1) // Future[String]

// Close the client
client.close()
```

## Server error mapping

* [Source](/test/examples/src/test/scala/test/examples/ServerErrorMapping.scala)

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
import automorph.{Default, Handler}
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
// Customize remote API server exception to RPC error mapping
val protocol = Default.protocol
val serverProtocol = protocol.mapException {
  case _: SQLException => InvalidRequest
  case e => protocol.exceptionToError(e)
}

// Start custom JSON-RPC HTTP server listening on port 80 for requests to '/api'
val system = Default.systemAsync
val handler = Handler.protocol(serverProtocol).system(system).context[Default.ServerContext]
val server = Default.server(handler, 80, "/api", mapException = {
  // Customize remote API server exception to HTTP status code mapping
  case _: SQLException => 400
  case e => HttpContext.defaultExceptionToStatusCode(e)
})

// Stop the server
server.close()
```

**Client**

```scala
// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientAsync(new URI("http://localhost/api"))

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
import zio.Task

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

// Start default JSON-RPC HTTP server listening on port 80 for requests to '/api'
val server = Default.serverSystem(system, 80, "/api")(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.client(system, new URI("http://localhost/api"))

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

// Start default REST-RPC HTTP server listening on port 80 for requests to '/api'
val system = Default.asyncSystem
val handler = Handler.protocol(protocol).system(system).context[Default.ServerContext]
val server = Default.server(handler, 80, "/api")

// Stop the server
server.close()
```

**Client**

```scala
// Setup default REST-RPC HTTP client sending POST requests to 'http://localhost/api'
val transport = Default.clientAsyncTransport(new URI("http://localhost/api"))
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
import codec.custom._
implicit def recordRw: codec.custom.ReadWriter[Record] = codec.custom.macroRW

// Create an RPC protocol plugin
val protocol = Default.protocol[UpickleMessagePackCodec.Node, codec.type](codec)

// Create an effect system plugin
val system = Default.asyncSystem

// Start default JSON-RPC HTTP server listening on port 80 for requests to '/api'
val handler = Handler.protocol(protocol).system(system).context[Default.ServerContext]
lazy val server = Default.server(handler.bind(api), 80, "/api")

// Stop the server
server.close()
```

**Client**

```scala
// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val transport = Default.clientAsyncTransport(new URI("http://localhost/api"))
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
// Start default JSON-RPC HTTP server listening on port 80 for requests to '/api'
val createServer = Default.serverSync(80, "/api")
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Create HttpUrlConnection HTTP client message transport
val transport = UrlClient(IdentitySystem(), new URI("http://localhost/api"))

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
// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientSync(new URI("http://localhost/api"))

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
val endpoint = UndertowHttpEndpoint(handler.bind(api))

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
// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientAsync(new URI("http://localhost/api"))

// Call the remote API function
val remoteApi = client.bind[Api] // Api
remoteApi.hello("world", 1) // Future[String]

// Close the client
client.close()
```

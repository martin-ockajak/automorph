# Examples

## Basic

### Synchronous call

* [Source](/test/examples/src/test/scala/test/examples/basic/SynchronousCall.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**Imports**

```scala
import automorph.Default
import automorph.transport.http.HttpMethod
import java.net.URI
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): String =
    s"Hello $some $n!"
}
val api = new ServerApi()

// Start default JSON-RPC HTTP server listening on port 8080 for POST requests to '/api'
val createServer = Default.serverSync(8080, "/api", Seq(HttpMethod.Post))
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): String
}

// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientSync(new URI("http://localhost/api"), HttpMethod.Post)

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // String

// Close the client
client.close()
```

### Asynchronous call

* [Source](/test/examples/src/test/scala/test/examples/basic/AsynchronousCall.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**Imports**

```scala
import automorph.Default
import automorph.transport.http.HttpMethod
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ServerApi()

// Start default JSON-RPC HTTP server listening on port 8080 for PUT requests to '/api'
val createServer = Default.serverAsync(8080, "/api", Seq(HttpMethod.Put))
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Setup default JSON-RPC HTTP client sending PUT requests to 'http://localhost/api'
val client = Default.clientAsync(new URI("http://localhost/api"), HttpMethod.Put)

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Future[String]

// Close the client
client.close()
```

### One-way message

* [Source](/test/examples/src/test/scala/test/examples/basic/OneWayMessage.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**Imports**

```scala
import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ServerApi()

// Start default JSON-RPC HTTP server listening on port 8080 for requests to '/api'
val createServer = Default.serverAsync(8080, "/api")
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.asyncHttpClient(new URI("http://localhost/api"))

// Message the remote API function dynamically without expecting a response
client.message("hello").args("some" -> "world", "n" -> 1) // Future[Unit]

// Close the client
client.close()
```

### HTTP request metadata

* [Source](/test/examples/src/test/scala/test/examples/basic/HttpRequestMetadata.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**Imports**

```scala
import automorph.Default
import automorph.Default.{ClientContext, ServerContext}
import java.net.URI

```

**Server**

```scala
// Create server API instance
class ServerApi {

  // Accept HTTP request context provided by the server message transport plugin
  def hello(message: String)(implicit http: ServerContext): String = Seq(
    Some(message),
    http.path,
    http.header("X-Test")
  ).flatten.mkString(", ")
}

val api = new ServerApi()

// Start default JSON-RPC HTTP server listening on port 8080 for requests to '/api'
val createServer = Default.serverSync(8080, "/api")
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {

  // Accept HTTP request context consumed by the client message transport plugin
  def hello(message: String)(implicit http: ClientContext): String
}

// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientSync(new URI("http://localhost/api"))

// Create client request context specifying HTTP request meta-data
implicit val http: ClientContext = client.defaultContext
  .parameters("test" -> "value")
  .headers("X-Test" -> "value", "Cache-Control" -> "no-cache")
  .cookies("Test" -> "value")
  .authorizationBearer("value")

// Call the remote API function statically with implicitly given request context
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("test") // String

// Call the remote API function dynamically with implicitly given request context
client.call[String]("hello").args("message" -> "test") // String

// Call the remote API function statically with directly supplied request context
remoteApi.hello("test")(using requestContext) // String

// Call the remote API function dynamically with directly supplied request context
client.call[String]("hello").args("message" -> "test")(using http) // String

// Close the client
client.close()
```

### HTTP response metadata

* [Source](/test/examples/src/test/scala/test/examples/basic/HttpResponseMetadata.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**Imports**

```scala
import automorph.Default.{ClientContext, ServerContext}
import automorph.transport.http.HttpContext
import automorph.{Contextual, Default}
import java.net.URI
```

**Server**

```scala

// Create server API instance
class ServerApi {

  // Return HTTP response context consumed by the server message transport plugin
  def hello(message: String): Contextual[String, ServerContext] = Contextual(
    message,
    HttpContext().headers("X-Test" -> "value", "Cache-Control" -> "no-cache")
  )
}
val api = new ServerApi()

// Start default JSON-RPC HTTP server listening on port 8080 for requests to '/api'
val createServer = Default.serverSync(8080, "/api")
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Define client view of the server API
trait ClientApi {

  // Return HTTP response context provided by the client message transport plugin
  def hello(message: String): Contextual[String, ClientContext]
}

// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientSync(new URI("http://localhost/api"))

// Call the remote API function expecting response context
val remoteApi = client.bind[ClientApi] // ClientApi
val static = remoteApi.hello("test") // Contextual[String, ClientContext]
static.result -> static.context.header("X-Test") // String -> "value"

// Call the remote API function dynamically expecting response context
val dynamic = client
  .call[Contextual[String, ClientContext]]("hello")
  .args("message" -> "test") // Contextual[String, ClientContext]
dynamic.result -> dynamic.context.header("X-Test") // String -> "value"

// Close the client
client.close()
```

## Customize

### Data serialization

* [Source](/test/examples/src/test/scala/test/examples/customize/CustomDataSerialization.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**Imports**

```scala
import automorph.Default
import io.circe.{Decoder, Encoder}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
```

**Data types**
```scala
// Introduce custom data types
sealed abstract class State
object State {
  case object On extends State
  case object Off extends State
}
case class Record(
  value: String,
  state: State
)

// Provide custom data type serialization and deserialization logic
import io.circe.generic.auto._
implicit lazy val enumEncoder: Encoder[State] = Encoder.encodeInt.contramap[State](Map(
  State.Off -> 0,
  State.On -> 1
))
implicit lazy val enumDecoder: Decoder[State] = Decoder.decodeInt.map(Map(
  0 -> State.Off,
  1 -> State.On
))
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int, record: Record): Future[Record] =
    Future(record.copy(value = s"Hello $some $n!"))
}
val api = new ServerApi()

// Start default JSON-RPC HTTP server listening on port 8080 for requests to '/api'
val createServer = Default.serverAsync(8080, "/api")
lazy val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int, record: Record): Future[Record]
}

// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientAsync(new URI("http://localhost/api"))

// Call the remote API function via proxy
lazy val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1, Record("test", State.On)) // Future[String]

// Close the client
client.close()
```

### Function names

* [Source](/test/examples/src/test/scala/test/examples/customize/FunctionNameMapping.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**Imports**

```scala
import automorph.Default
import java.net.URI
import scala.util.Try
```

**Server**

```scala
// Create server API instance
class ServerApi {
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
val api = new ServerApi()

// Customize RPC function names
val mapName = (name: String) => name match {
  case "hello" => Seq("hello", "custom")
  case "omitted" => Seq.empty
  case other => Seq(s"test.$other")
}

// Start default JSON-RPC HTTP server listening on port 8080 for requests to '/api'
val createServer = Default.serverSync(8080, "/api")
val server = createServer(_.bind(api, mapName(_)))

// Stop the server
server.close()
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(value: Option[String]): String
  
  def custom(value: Option[String]): String
}

// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientSync(new URI("http://localhost/api"))

// Call the remote API function statically
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello(None) // ""
remoteApi.custom(None) // ""

// Call the remote API function dynamically
Try(client.call[String]("omitted").args()) // Failure
client.call[Double]("test.multi").args("add" -> true, "n" -> 1) // 2

// Close the client
client.close()
```

### Client errors

* [Source](/test/examples/src/test/scala/test/examples/customize/ClientErrorMapping.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**Imports**

```scala
import automorph.{Client, Default}
import java.net.URI
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ServerApi()

// Start default JSON-RPC HTTP server listening on port 8080 for requests to '/api'
val createServer = Default.serverAsync(8080, "/api")
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Customize remote API client RPC error to exception mapping
val protocol = Default.protocol.mapError((message, code) =>
  if (message.contains("SQL")) {
    new SQLException(message)
  } else {
    Default.protocol.mapError(message, code)
  }
)

// Setup custom JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val transport = Default.clientTransportAsync(new URI("http://localhost/api"))
val client = Client.protocol(protocol).transport(transport)

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Future[String]

// Close the client
client.close()
```

### Server errors

* [Source](/test/examples/src/test/scala/test/examples/customize/ServerErrorMapping.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**Imports**

```scala
import automorph.protocol.jsonrpc.ErrorType.InvalidRequest
import automorph.{Default, Handler}
import java.net.URI
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ServerApi()

// Customize remote API server exception to RPC error mapping
val protocol = Default.protocol
val serverProtocol = protocol.mapException {
  case _: SQLException => InvalidRequest
  case e => protocol.mapException(e)
}

// Start custom JSON-RPC HTTP server listening on port 8080 for requests to '/api'
val system = Default.systemAsync
val handler = Handler.protocol(serverProtocol).system(system)
  .context[Default.ServerContext]
val server = Default.server(handler, 8080, "/api")

// Stop the server
server.close()
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientAsync(new URI("http://localhost/api"))

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Future[String]

// Close the client
client.close()
```

### HTTP response status

* [Source](/test/examples/src/test/scala/test/examples/customize/HttpResponseStatusMapping.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**Imports**

```scala
import automorph.Default
import automorph.transport.http.HttpContext
import java.net.URI
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ServerApi()

// Customize remote API server exception to HTTP status code mapping
val createServer = Default.serverAsync(8080, "/api", mapException = {
  case _: SQLException => 400
  case e => HttpContext.defaultExceptionToStatusCode(e)
})

// Start custom JSON-RPC HTTP server listening on port 8080 for requests to '/api'
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientAsync(new URI("http://localhost/api"))

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Future[String]

// Close the client
client.close()
```

## Select

### Effect system

* [Source](/test/examples/src/test/scala/test/examples/select/EffectSystemSelection.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1",
  "org.automorph" %% "automorph-zio" % "0.0.1",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % "3.3.9"
)
```

**Imports**

```scala
import automorph.Default
import automorph.system.ZioSystem
import java.net.URI
import zio.Task
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Task[String] =
    Task.succeed(s"Hello $some $n!")
}
val api = new ServerApi()

// Create an effect system plugin
val system = ZioSystem[Any]()

// Start default JSON-RPC HTTP server listening on port 8080 for requests to '/api'
val server = Default.serverSystem(system, 8080, "/api")(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Task[String]
}

// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.client(system, new URI("http://localhost/api"))

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Task[String]

// Close the client
client.close()
```

### RPC protocol

* [Source](/test/examples/src/test/scala/test/examples/select/RpcProtocolSelection.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**Imports**

```scala
import automorph.protocol.RestRpcProtocol
import automorph.{Client, Default, Handler}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ServerApi()

// Create REST-RPC protocol plugin
val protocol = RestRpcProtocol[Default.Node, Default.Codec](Default.codec)

// Start default REST-RPC HTTP server listening on port 8080 for requests to '/api'
val system = Default.asyncSystem
val handler = Handler.protocol(protocol).system(system).context[Default.ServerContext]
val server = Default.server(handler, 8080, "/api")

// Stop the server
server.close()
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Setup default REST-RPC HTTP client sending POST requests to 'http://localhost/api'
val transport = Default.clientAsyncTransport(new URI("http://localhost/api"))
val client = Client.protocol(protocol).transport(transport)

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Future[String]

// Close the client
client.close()
```

### Message codec

* [Source](/test/examples/src/test/scala/test/examples/select/MessageCodecSelection.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1",
  "org.automorph" %% "automorph-upickle" % "0.0.1"
)
```

**Imports**

```scala
import automorph.codec.messagepack.UpickleMessagePackCodec
import automorph.{Client, Default, Handler}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
```

**Data types**
```scala
// Introduce custom data types
case class Record(values: List[String])

// Create uPickle message codec for JSON format
val codec = UpickleMessagePackCodec()

// Provide custom data type serialization and deserialization logic
import codec.custom._
implicit def recordRw: codec.custom.ReadWriter[Record] = codec.custom.macroRW
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[Record] =
    Future(Record(List("Hello", some, n.toString)))
}
val api = new ServerApi()

// Create an RPC protocol plugin
val protocol = Default.protocol[UpickleMessagePackCodec.Node, codec.type](codec)

// Create an effect system plugin
val system = Default.asyncSystem

// Start default JSON-RPC HTTP server listening on port 8080 for requests to '/api'
val handler = Handler.protocol(protocol).system(system).context[Default.ServerContext]
lazy val server = Default.server(handler.bind(api), 8080, "/api")

// Stop the server
server.close()
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[Record]
}

// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val transport = Default.clientAsyncTransport(new URI("http://localhost/api"))
val client = Client(protocol, transport)

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Future[String]

// Close the client
client.close()
```

### Client transport

* [Source](/test/examples/src/test/scala/test/examples/select/ClientTransportSelection.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**Imports**

```scala
import automorph.Default
import automorph.system.IdentitySystem
import automorph.transport.http.client.UrlClient
import java.net.URI
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): String =
    s"Hello $some $n!"
}
val api = new ServerApi()

// Start default JSON-RPC HTTP server listening on port 8080 for requests to '/api'
val createServer = Default.serverSync(8080, "/api")
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): String
}

// Create HttpUrlConnection HTTP client message transport
val transport = UrlClient(IdentitySystem(), new URI("http://localhost/api"))

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.client(transport)

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // String

// Close the client
client.close()
```

### Server transport

* [Source](/test/examples/src/test/scala/test/examples/select/ServerTransportSelection.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**Imports**

```scala
import automorph.Default
import automorph.transport.http.server.NanoServer
import java.net.URI
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): String =
    s"Hello $some $n!"
}
val api = new ServerApi()

// Start NanoHTTPD JSON-RPC HTTP server listening on port 8080 for requests to '/api'
val handler = Default.handlerSync[NanoServer.Context]
val createServer = NanoServer.create(handler.bind(api), 8080)
val server = createServer(identity)

// Stop the server
server.close()
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): String
}

// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientSync(new URI("http://localhost/api"))

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // String

// Close the client
client.close()
```

### Endpoint transport

* [Source](/test/examples/src/test/scala/test/examples/select/EndpointTransportSelection.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**Imports**

```scala
import automorph.Default
import automorph.transport.http.endpoint.UndertowHttpEndpoint
import io.undertow.{Handlers, Undertow}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ServerApi()

// Create custom Undertow JSON-RPC endpoint
val handler = Default.handlerAsync[UndertowHttpEndpoint.Context]
val endpoint = UndertowHttpEndpoint(handler.bind(api))

// Start Undertow JSON-RPC HTTP server listening on port 8080 for requests to '/api'
val server = Undertow.builder()
  .addHttpListener(8080, "0.0.0.0")
  .setHandler(Handlers.path().addPrefixPath("/api", endpoint))
  .build()

// Stop the server
server.stop()
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientAsync(new URI("http://localhost/api"))

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Future[String]

// Close the client
client.close()
```

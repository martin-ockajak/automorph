# Examples

Most of the following examples are using [default plugins](../plugins/README.md).

## Basic

### Synchronous call

* [Source](/examples/project/src/test/scala/examples/basic/SynchronousCall.scala)

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

// Start JSON-RPC HTTP server listening on port 7000 for POST requests to '/api'
val createServer = Default.serverSync(7000, "/api", Seq(HttpMethod.Post))
val server = createServer(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): String
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientSync(new URI("http://localhost:7000/api"), HttpMethod.Post)

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // String
```

**Cleanup**

```scala
// Close the client
client.close()

// Stop the server
server.close()
```

### Asynchronous call

* [Source](/examples/project/src/test/scala/examples/basic/AsynchronousCall.scala)

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
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ServerApi()

// Start JSON-RPC HTTP server listening on port 7000 for PUT requests to '/api'
val createServer = Default.serverAsync(7000, "/api", Seq(HttpMethod.Put))
val server = createServer(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Setup JSON-RPC HTTP client sending PUT requests to 'http://localhost:7000/api'
val client = Default.clientAsync(new URI("http://localhost:7000/api"), HttpMethod.Put)

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Future[String]
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

### API description

* [Source](/examples/project/src/test/scala/examples/basic/ApiSpecification.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**Imports**

```scala
import automorph.Default
import automorph.description.{OpenApi, OpenRpc}
import automorph.protocol.JsonRpcProtocol
import automorph.transport.http.HttpMethod
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ServerApi()

// Start JSON-RPC HTTP server listening on port 7000 for PUT requests to '/api'
val createServer = Default.serverAsync(7000, "/api", Seq(HttpMethod.Put))
val server = createServer(_.bind(api))
```

**Client**

```scala
// Setup JSON-RPC HTTP client sending PUT requests to 'http://localhost:7000/api'
val client = Default.clientAsync(new URI("http://localhost:7000/api"), HttpMethod.Put)

// Retrieve remote API description in OpenRPC format
val openRpcFunction = JsonRpcProtocol.openRpcFunction
val openRpc = client.call[OpenRpc](openRpcFunction).args() // Future[OpenRpc]
println(Await.result(openRpc, Duration.Inf))

// Retrieve remote API description in OpenAPI format
val openApiFunction = JsonRpcProtocol.openApiFunction
val openApi = client.call[OpenApi](openApiFunction).args() // Future[OpenApi]
println(Await.result(openApi, Duration.Inf))
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

### One-way message

* [Source](/examples/project/src/test/scala/examples/basic/OneWayMessage.scala)

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
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ServerApi()

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val createServer = Default.serverAsync(7000, "/api")
val server = createServer(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.asyncHttpClient(new URI("http://localhost:7000/api"))

// Message the remote API function dynamically without expecting a response
client.message("hello").args("some" -> "world", "n" -> 1) // Future[Unit]
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

### Optional parameters

* [Source](/examples/project/src/test/scala/examples/basic/OptionalParameters.scala)

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
  def hello(some: String, n: Option[Int]): String =
    s"Hello $some ${n.getOrElse(0)}!"

  def hi(some: Option[String])(n: Int): String =
    s"Hi ${some.getOrElse("all")} $n!"
}
val api = new ServerApi()

// Start JSON-RPC HTTP server listening on port 7000 for PUT requests to '/api'
val createServer = Default.serverSync(7000, "/api", Seq(HttpMethod.Put))
val server = createServer(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String): String
}
// Setup JSON-RPC HTTP client sending PUT requests to 'http://localhost:7000/api'
val client = Default.clientSync(new URI("http://localhost:7000/api"), HttpMethod.Put)

// Call the remote API function statically
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world") // String

// Call the remote API function dynamically
client.call[String]("hi").args("n" -> 1) // String
```

**Cleanup**

```scala
// Close the client
client.close()

// Stop the server
server.close()
```

### HTTP request metadata

* [Source](/examples/project/src/test/scala/examples/basic/HttpRequestMetadata.scala)

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

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val createServer = Default.serverSync(7000, "/api")
val server = createServer(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {

  // Accept HTTP request context consumed by the client message transport plugin
  def hello(message: String)(implicit http: ClientContext): String
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientSync(new URI("http://localhost:7000/api"))

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
```

**Cleanup**

```scala
// Close the client
client.close()

// Stop the server
server.close()
```

### HTTP response metadata

* [Source](/examples/project/src/test/scala/examples/basic/HttpResponseMetadata.scala)

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

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val createServer = Default.serverSync(7000, "/api")
val server = createServer(_.bind(api))
```

**Client**

```scala
// Define client view of the server API
trait ClientApi {

  // Return HTTP response context provided by the client message transport plugin
  def hello(message: String): Contextual[String, ClientContext]
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientSync(new URI("http://localhost:7000/api"))

// Call the remote API function expecting response context
val remoteApi = client.bind[ClientApi] // ClientApi
val static = remoteApi.hello("test") // Contextual[String, ClientContext]
static.result -> static.context.header("X-Test") // String -> "value"

// Call the remote API function dynamically expecting response context
val dynamic = client
  .call[Contextual[String, ClientContext]]("hello")
  .args("message" -> "test") // Contextual[String, ClientContext]
dynamic.result -> dynamic.context.header("X-Test") // String -> "value"
```

**Cleanup**

```scala
// Close the client
client.close()

// Stop the server
server.close()
```

## Customize

### Data serialization

* [Source](/examples/project/src/test/scala/examples/customize/CustomDataSerialization.scala)

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
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
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

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val createServer = Default.serverAsync(7000, "/api")
lazy val server = createServer(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int, record: Record): Future[Record]
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientAsync(new URI("http://localhost:7000/api"))

// Call the remote API function via proxy
lazy val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1, Record("test", State.On)) // Future[String]
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

### Client function names

* [Source](/examples/project/src/test/scala/examples/customize/ClientFunctionNameMapping.scala)

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
```

**Server**

```scala
// Create server API instance
class ServerApi {
  // Exposed both as 'hello' and 'hi'
  def hello(some: String, n: Int): String =
    s"Hello $some $n!"
}
val api = new ServerApi()

// Customize exposed API to RPC function name mapping
val mapName = (name: String) => name match {
  case "hello" => Seq("hello", "hi")
  case "skip" => Seq.empty
  case other => Seq(s"test.$other")
}

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val createServer = Default.serverSync(7000, "/api")
val server = createServer(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hi(some: String, n: Int): String
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientSync(new URI("http://localhost:7000/api"))

// Customize invoked API to RPC function name mapping
val mapName = (name: String) => name match {
  case "hi" => "hello"
  case other => other
}

// Call the remote API function
val remoteApi = client.bind[ClientApi](mapName) // ClientApi
remoteApi.hi("world", 1) // String
```

**Cleanup**

```scala
// Close the client
client.close()

// Stop the server
server.close()
```

### Server function names

* [Source](/examples/project/src/test/scala/examples/customize/ServerFunctionNameMapping.scala)

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
  // Exposed both as 'hello' and 'hi'
  def hello(some: String, n: Int): String =
    s"Hello $some $n!"

  // Not exposed
  def skip(): String =
    ""

  // Exposed as 'test.welcome'
  def welcome(add: Boolean)(n: Double): Double =
    if (add) n + 1 else n - 1
}
val api = new ServerApi()

// Customize exposed API to RPC function name mapping
val mapName = (name: String) => name match {
  case "hello" => Seq("hello", "hi")
  case "skip" => Seq.empty
  case other => Seq(s"test.$other")
}

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val createServer = Default.serverSync(7000, "/api")
val server = createServer(_.bind(api, mapName))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): String

  def hi(some: String, n: Int): String
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientSync(new URI("http://localhost:7000/api"))

// Call the remote API function statically
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // String
remoteApi.hi("world", 1) // String

// Call the remote API function dynamically
Try(client.call[String]("skip").args()) // Failure
client.call[Double]("test.welcome").args("add" -> true, "n" -> 1) // Double
```

**Cleanup**

```scala
// Close the client
client.close()

// Stop the server
server.close()
```

### Client errors

* [Source](/examples/project/src/test/scala/examples/customize/ClientErrorMapping.scala)

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
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ServerApi()

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val createServer = Default.serverAsync(7000, "/api")
val server = createServer(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Customize remote API client RPC error to exception mapping
val protocol = Default.protocol[Default.ClientContext].mapError((message, code) =>
  if (message.contains("SQL")) {
    new SQLException(message)
  } else {
    Default.protocol.mapError(message, code)
  }
)

// Setup custom JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val transport = Default.clientTransportAsync(new URI("http://localhost:7000/api"))
val client = Client.protocol(protocol).transport(transport)

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Future[String]
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

### Server errors

* [Source](/examples/project/src/test/scala/examples/customize/ServerErrorMapping.scala)

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
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    if (n >= 0) {
      Future.failed(SQLException("Data error"))
    } else {
      Future.failed(JsonRpcException("Other error", 1))
    }
}
val api = new ServerApi()

// Customize remote API server exception to RPC error mapping
val protocol = Default.protocol[Default.ServerContext].mapException {
  case _: SQLException => InvalidRequest
  case e => Default.protocol.mapException(e)
}

// Start custom JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val system = Default.systemAsync
val handler = Handler.protocol(protocol).system(system)
val server = Default.server(handler, 7000, "/api")
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientAsync(new URI("http://localhost:7000/api"))

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // JSON-RPC invalid request error with code: -32600
remoteApi.hello("world", -1) // JSON-RPC application error with code: 1
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```
### Arguments by position

* [Source](/examples/project/src/test/scala/examples/basic/ArgumentsByPosition.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

**Imports**

```scala
import automorph.{Client, Default}
import automorph.transport.http.HttpMethod
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ServerApi()

// Start JSON-RPC HTTP server listening on port 7000 for PUT requests to '/api'
val createServer = Default.serverAsync(7000, "/api", Seq(HttpMethod.Put))
val server = createServer(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Configure JSON-RPC to pass arguments by position instead of by name
val protocol = Default.protocol[Default.ClientContext].namedArguments(false)

// Setup custom JSON-RPC HTTP client sending PUT requests to 'http://localhost:7000/api'
val url = new URI("http://localhost:7000/api")
val clientTransport = Default.clientTransportAsync(url, HttpMethod.Put)
val client = Client.protocol(protocol).transport(clientTransport)

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Future[String]
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

### HTTP response status

* [Source](/examples/project/src/test/scala/examples/customize/HttpResponseStatusMapping.scala)

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
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
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
val createServer = Default.serverAsync(7000, "/api", mapException = {
  case _: SQLException => 400
  case e => HttpContext.defaultExceptionToStatusCode(e)
})

// Start custom JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val server = createServer(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientAsync(new URI("http://localhost:7000/api"))

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Future[String]
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

## Select

### RPC protocol

* [Source](/examples/project/src/test/scala/examples/select/RpcProtocolSelection.scala)

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
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ServerApi()

// Create a server REST-RPC protocol plugin with '/api' path prefix
val serverProtocol = RestRpcProtocol(Default.codec, "/api/" )
  .context[Default.ServerContext]

// Start default REST-RPC HTTP server listening on port 7000 for requests to '/api'
val system = Default.systemAsync
val handler = Handler.protocol(serverProtocol).system(system)
val server = Default.server(handler, 7000, "/api")
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Create a client REST-RPC protocol plugin with '/api' path prefix
val clientProtocol = RestRpcProtocol(Default.codec, "/api/")
  .context[Default.ClientContext]

// Setup default REST-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val transport = Default.clientTransportAsync(new URI("http://localhost:7000/api"))
val client = Client.protocol(clientProtocol).transport(transport)

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Future[String]
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

### Effect system

* [Source](/examples/project/src/test/scala/examples/select/EffectSystemSelection.scala)

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
import zio.{Runtime, Task}
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

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val server = Default.serverSystem(system, 7000, "/api")(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Task[String]
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.client(system, new URI("http://localhost:7000/api"))

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Task[String]
```

**Cleanup**

```scala
// Close the client
Runtime.default.unsafeRunTask(client.close())

// Stop the server
Runtime.default.unsafeRunTask(server.close())
```

### Message codec

* [Source](/examples/project/src/test/scala/examples/select/MessageCodecSelection.scala)

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
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
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

// Create a server RPC protocol plugin
val serverProtocol =
  Default.protocol[UpickleMessagePackCodec.Node, codec.type, Default.ServerContext](
    codec
  )

// Create an effect system plugin
val system = Default.systemAsync

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val handler = Handler.protocol(serverProtocol).system(system)
lazy val server = Default.server(handler.bind(api), 7000, "/api")
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[Record]
}

// Create a client RPC protocol plugin
val clientProtocol =
  Default.protocol[UpickleMessagePackCodec.Node, codec.type, Default.ClientContext](
    codec
  )

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val transport = Default.clientTransportAsync(new URI("http://localhost:7000/api"))
val client = Client(clientProtocol, transport)

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Future[String]
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

### Client transport

* [Source](/examples/project/src/test/scala/examples/select/ClientTransportSelection.scala)

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

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val createServer = Default.serverSync(7000, "/api")
val server = createServer(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): String
}

// Create HttpUrlConnection HTTP client message transport
val transport = UrlClient(IdentitySystem(), new URI("http://localhost:7000/api"))

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.client(transport)

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // String
```

**Cleanup**

```scala
// Close the client
client.close()

// Stop the server
server.close()
```

### Server transport

* [Source](/examples/project/src/test/scala/examples/select/ServerTransportSelection.scala)

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

// Start NanoHTTPD JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val handler = Default.handlerSync[NanoServer.Context]
val createServer = NanoServer.create(handler.bind(api), 7000)
val server = createServer(identity)
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): String
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientSync(new URI("http://localhost:7000/api"))

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // String
```

**Cleanup**

```scala
// Close the client
client.close()

// Stop the server
server.close()
```

### [Endpoint transport]

* [Source](/examples/project/src/test/scala/examples/select/EndpointTransportSelection.scala)

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
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
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

// Start Undertow JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val server = Undertow.builder()
  .addHttpListener(7000, "0.0.0.0")
  .setHandler(Handlers.path().addPrefixPath("/api", endpoint))
  .build()
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientAsync(new URI("http://localhost:7000/api"))

// Call the remote API function
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Future[String]
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

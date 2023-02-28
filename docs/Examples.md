---
sidebar_position: 5
---

# Examples

Most of the following examples are using [default plugins](Plugins).


## Basic

### [Synchronous call](../../examples/project/src/main/scala/examples/basic/SynchronousCall.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
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
  def hello(some: String, n: Int): String =
    s"Hello $some $n!"
}
val api = new ServerApi()

// Start JSON-RPC HTTP server listening on port 7000 for POST requests to '/api'
val serverBuilder = Default.serverSync(7000, "/api")
val server = serverBuilder(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): String
}
// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientSync(new URI("http://localhost:7000/api"))

// Call the remote API function statically
val remoteApi = client.bind[ClientApi]
println(
  remoteApi.hello("world", 1)
)

// Call the remote API function dynamically
println(
  client.call[String]("hello").args("some" -> "world", "n" -> 1)
)
```

**Cleanup**

```scala
// Close the client
client.close()

// Stop the server
server.close()
```

### [Asynchronous call](../../examples/project/src/main/scala/examples/basic/AsynchronousCall.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
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

// Start JSON-RPC HTTP server listening on port 7000 for POST or PUT requests to '/api'
val serverBuilder = Default.serverBuilderAsync(7000, "/api", Seq(HttpMethod.Post, HttpMethod.Put))
val server = serverBuilder(_.bind(api))
```

**Client**

```scala
// Define client view of the remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}
// Setup JSON-RPC HTTP client sending PUT requests to 'http://localhost:7000/api'
val client = Default.clientAsync(new URI("http://localhost:7000/api"), HttpMethod.Put)

// Call the remote API function statically
val remoteApi = client.bind[ClientApi]
println(Await.result(
  remoteApi.hello("world", 1),
  Duration.Inf
))

// Call the remote API function dynamically
println(Await.result(
  client.call[String]("hello").args("some" -> "world", "n" -> 1),
  Duration.Inf
))
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

### [Optional parameters](../../examples/project/src/main/scala/examples/basic/OptionalParameters.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
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
  def hello(some: String, n: Option[Int]): String =
    s"Hello $some ${n.getOrElse(0)}!"

  def hi(some: Option[String])(n: Int): String =
    s"Hi ${some.getOrElse("who" -> "all")} $n!"
}
val api = new ServerApi()

// Start JSON-RPC HTTP server listening on port 7000 for POST requests to '/api'
val serverBuilder = Default.serverSync(7000, "/api")
val server = serverBuilder(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String): String
}
// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientSync(new URI("http://localhost:7000/api"))

// Call the remote API function statically
val remoteApi = client.bind[ClientApi]
println(
  remoteApi.hello("world")
)

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


## Customization

### [Data serialization](../../examples/project/src/main/scala/examples/customization/DataSerialization.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
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
import io.circe.generic.auto.*
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
  def hello(some: String, record: Record): Future[Record] =
    Future(record.copy(value = s"Hello $some!"))
}
val api = new ServerApi()

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val serverBuilder = Default.serverAsync(7000, "/api")
val server = serverBuilder(_.bind(api))
```

**Client**

```scala
// Define client view of the remote API
trait ClientApi {
  def hello(some: String, record: Record): Future[Record]
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientAsync(new URI("http://localhost:7000/api"))

// Call the remote API function via proxy
lazy val remoteApi = client.bind[ClientApi]
println(Await.result(
  remoteApi.hello("world", Record("test", State.On)),
  Duration.Inf
))
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

### [Client function names](../../examples/project/src/main/scala/examples/customization/ClientFunctionNames.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
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

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val serverBuilder = Default.serverSync(7000, "/api")
val server = serverBuilder(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): String

  // Invoked as 'hello'
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
val remoteApi = client.bind[ClientApi](mapName)
println(
  remoteApi.hello("world", 1)
)
println(
  remoteApi.hi("world", 1)
)
```

**Cleanup**

```scala
// Close the client
client.close()

// Stop the server
server.close()
```

### [Server function names](../../examples/project/src/main/scala/examples/customization/ServerFunctionNames.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
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

  // Exposed as 'test.sum'
  def sum(numbers: List[Double]): Double =
    numbers.sum

  // Not exposed
  def hidden(): String =
    ""
}
val api = new ServerApi()

// Customize exposed API to RPC function name mapping
val mapName = (name: String) => name match {
  case "hello" => Seq("hello", "hi")
  case "hidden" => Seq.empty
  case other => Seq(s"test.$other")
}

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val serverBuilder = Default.serverBuilderSync(7000, "/api")
val server = serverBuilder(_.bind(api, mapName))
```

**Client**

```scala
// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientSync(new URI("http://localhost:7000/api"))

// Call the remote API function statically
val remoteApi = client.bind[ClientApi]
println(
  remoteApi.hello("world", 1)
)
println(
  remoteApi.hi("world", 1)
)

// Call the remote API function dynamically
println(
  client.call[Double]("test.sum").args("numbers" -> List(1, 2, 3))
)

// Call the remote API function dynamically and fail with FunctionNotFoundException
println(Try(
  client.call[String]("hidden").args()
).failed.get)
```

**Cleanup**

```scala
// Close the client
client.close()

// Stop the server
server.close()
```


## Errors

### [Client exceptions](../../examples/project/src/main/scala/examples/errors/ClientExceptions.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
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
import scala.util.Try
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future.failed(new IllegalArgumentException("SQL error"))
}
val api = new ServerApi()

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val serverBuilder = Default.serverBuilderAsync(7000, "/api")
val server = serverBuilder(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Customize remote API client RPC error to exception mapping
val rpcProtocol = Default.protocol[Default.ClientContext].mapError((message, code) =>
  if (message.contains("SQL")) {
    new SQLException(message)
  } else {
    Default.protocol.mapError(message, code)
  }
)

// Setup custom JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val clientTransport = Default.clientTransportAsync(new URI("http://localhost:7000/api"))
val client = Client.protocol(rpcProtocol).transport(clientTransport)

// Call the remote API function and fail with SQLException
val remoteApi = client.bind[ClientApi]
println(Try(Await.result(
  remoteApi.hello("world", 1),
  Duration.Inf
)).failed.get)
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

### [Server errors](../../examples/project/src/main/scala/examples/errors/ServerErrors.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
)
```

**Imports**

```scala
import automorph.protocol.jsonrpc.ErrorType.InvalidRequest
import automorph.protocol.jsonrpc.JsonRpcException
import automorph.{Default, Handler}
import java.net.URI
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    if (n >= 0) {
      Future.failed(new SQLException("Invalid request"))
    } else {
      Future.failed(JsonRpcException("Application error", 1))
    }
}

val api = new ServerApi()

// Customize remote API server exception to RPC error mapping
val rpcProtocol = Default.protocol[Default.ServerContext].mapException(_ match {
  case _: SQLException => InvalidRequest
  case error => Default.protocol.mapException(error)
})

// Start custom JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val handler = Handler.protocol(rpcProtocol).system(Default.effectSystemAsync).bind(api)
val server = Default.server(handler, 7000, "/api")
```

**Client**

```scala
// Define client view of the remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientAsync(new URI("http://localhost:7000/api"))

// Call the remote API function and fail with InvalidRequestException
val remoteApi = client.bind[ClientApi]
println(Try(Await.result(
  remoteApi.hello("world", 1),
  Duration.Inf
)).failed.get)

// Call the remote API function and fail with RuntimeException
println(Try(Await.result(
  remoteApi.hello("world", -1),
  Duration.Inf
)).failed.get)
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

### [HTTP status code](../../examples/project/src/main/scala/examples/errors/HttpStatusCode.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
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
import scala.util.Try
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future.failed(new SQLException("Invalid request"))
}
val api = new ServerApi()

// Customize remote API server exception to HTTP status code mapping
val serverBuilder = Default.serverAsync(7000, "/api", mapException = {
  case _: SQLException => 400
  case e => HttpContext.defaultExceptionToStatusCode(e)
})

// Start custom JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val server = serverBuilder(_.bind(api))
```

**Client**

```scala
// Define client view of the remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}
// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientAsync(new URI("http://localhost:7000/api"))

// Call the remote API function and fail with InvalidRequestException
val remoteApi = client.bind[ClientApi]
println(Try(Await.result(
  remoteApi.hello("world", 1),
  Duration.Inf
)).failed.get)
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```


## Metadata

### [HTTP authentication](../../examples/project/src/main/scala/examples/metadata/HttpAuthentication.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
)
```

**Imports**

```scala
import automorph.Default
import automorph.Default.{ClientContext, ServerContext}
import java.net.URI
import scala.util.Try
```

**Server**

```scala
// Create server API instance
class ServerApi {

  // Accept HTTP request context provided by the server message transport plugin
  def hello(message: String)(implicit httpRequest: ServerContext): String =
    httpRequest.authorizationBearer match {
      case Some("valid") => s"Hello $message!"
      case _ => throw new IllegalAccessException("Authentication failed")
    }
}
val api = new ServerApi()

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val serverBuilder = Default.serverBuilderSync(7000, "/api")
val server = serverBuilder(_.bind(api))
```

**Client**

```scala
// Define client view of the remote API
trait ClientApi {

  // Accept HTTP request context consumed by the client message transport plugin
  def hello(message: String)(implicit http: ClientContext): String
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientSync(new URI("http://localhost:7000/api"))
val remoteApi = client.bind[ClientApi]

{
  // Create client request context containing invalid HTTP authentication
  implicit val validAuthentication: ClientContext = client.defaultContext
    .authorizationBearer("valid")

  // Call the remote API function statically using valid authentication
  println(
    remoteApi.hello("test")
  )

  // Call the remote API function dynamically using valid authentication
  println(
    client.call[String]("hello").args("message" -> "test")
  )
}

{
  // Create client request context containing invalid HTTP authentication
  implicit val invalidAuthentication: ClientContext = client.defaultContext
    .headers("X-Authentication" -> "unsupported")

  // Call the remote API function statically using invalid authentication
  println(Try(
    remoteApi.hello("test")
  ).failed.get)

  // Call the remote API function dynamically using invalid authentication
  println(Try(
    client.call[String]("hello").args("message" -> "test")
  ).failed.get)
}
```

**Cleanup**

```scala
// Close the client
client.close()

// Stop the server
server.close()
```

### [HTTP request](../../examples/project/src/main/scala/examples/metadata/HttpRequest.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
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
  def hello(message: String)(implicit httpRequest: ServerContext): String =
    Seq(
      Some(message),
      httpRequest.path,
      httpRequest.header("X-Test")
    ).flatten.mkString(", ")
}
val api = new ServerApi()

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val serverBuilder = Default.serverBuilderSync(7000, "/api")
val server = serverBuilder(_.bind(api))
```

**Client**

```scala
// Define client view of the remote API
trait ClientApi {

  // Accept HTTP request context consumed by the client message transport plugin
  def hello(message: String)(implicit http: ClientContext): String
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientSync(new URI("http://localhost:7000/api"))

// Create client request context specifying HTTP request metadata
implicit val httpRequest: ClientContext = client.defaultContext
  .parameters("test" -> "value")
  .headers("X-Test" -> "value", "Cache-Control" -> "no-cache")
  .cookies("Test" -> "value")
  .authorizationBearer("value")

// Call the remote API function statically using implicitly given HTTP request metadata
val remoteApi = client.bind[ClientApi]
println(
  remoteApi.hello("test")
)

// Call the remote API function dynamically using implicitly given HTTP request metadata
println(
  client.call[String]("hello").args("message" -> "test")
)
```

**Cleanup**

```scala
// Close the client
client.close()

// Stop the server
server.close()
```

### [HTTP response](../../examples/project/src/main/scala/examples/metadata/HttpResponse.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
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
    HttpContext().headers("X-Test" -> "value", "Cache-Control" -> "no-cache").statusCode(200)
  )
}
val api = new ServerApi()

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val serverBuilder = Default.serverSync(7000, "/api")
val server = serverBuilder(_.bind(api))
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

// Call the remote API function retrieving a result with HTTP response metadata
val remoteApi = client.bind[ClientApi]
val static = remoteApi.hello("test")
println(static.result)
println(static.context.header("X-Test"))

// Call the remote API function dynamically retrieving a result with HTTP response metadata
val dynamic = client.call[Contextual[String, ClientContext]]("hello").args("message" -> "test")
println(dynamic.result)
println(dynamic.context.header("X-Test"))
```

**Cleanup**

```scala
// Close the client
client.close()

// Stop the server
server.close()
```


## Special

### [API schema](../../examples/project/src/main/scala/examples/special/ApiSchema.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
)
```

**Imports**

```scala
import automorph.Default
import automorph.schema.{OpenApi, OpenRpc}
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

// Start JSON-RPC HTTP server listening on port 7000 for POST requests to '/api'
val serverBuilder = Default.serverBuilderAsync(7000, "/api")
val server = serverBuilder(_.bind(api))
```

**Client**

```scala
// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientAsync(new URI("http://localhost:7000/api"))

// Retrieve the remote API schema in OpenRPC format
println(Await.result(
  client.call[OpenRpc](JsonRpcProtocol.openRpcFunction).args(),
  Duration.Inf
).methods.map(_.name))

// Retrieve the remote API schema in OpenAPI format
println(Await.result(
  client.call[OpenApi](JsonRpcProtocol.openApiFunction).args(),
  Duration.Inf
).paths.get.keys.toList)
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

### [Dynamic payload](../../examples/project/src/main/scala/examples/special/DynamicPayload.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
)
```

**Imports**

```scala
import automorph.Default
import io.circe.Json
import java.net.URI
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: Json, n: Int): Json =
    if (some.isString) {
      val value = some.as[String].toTry.get
      Json.fromString(s"Hello $value $n!")
    } else {
      Json.fromValues(Seq(some, Json.fromInt(n)))
    }
}
val api = new ServerApi()

// Start JSON-RPC HTTP server listening on port 7000 for PUT requests to '/api'
val serverBuilder = Default.serverBuilderSync(7000, "/api")
val server = serverBuilder(_.bind(api))
```

**Client**

```scala
// Define client view of the remote API
trait ClientApi {
  def hello(some: String, n: Json): Json
}

// Setup JSON-RPC HTTP client sending PUT requests to 'http://localhost:7000/api'
val client = Default.clientSync(new URI("http://localhost:7000/api"))

// Call the remote API function statically
val remoteApi = client.bind[ClientApi]
println(
  remoteApi.hello("world", Json.fromInt(1))
)

// Call the remote API function dynamically
println(
  client.call[Seq[Int]]("hello").args("some" -> Json.fromInt(0), "n" -> 1)
)
```

**Cleanup**

```scala
// Close the client
client.close()

// Stop the server
server.close()
```

### [One-way message](../../examples/project/src/main/scala/examples/special/OneWayMessage.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
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
val serverBuilder = Default.serverAsync(7000, "/api")
val server = serverBuilder(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}
// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientAsync(new URI("http://localhost:7000/api"))

// Message the remote API function dynamically without expecting a response
Await.result(
  client.message("hello").args("some" -> "world", "n" -> 1),
  Duration.Inf
)
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

### [Positional arguments](../../examples/project/src/main/scala/examples/special/PositionalArguments.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
)
```

**Imports**

```scala
import automorph.{Client, Default}
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

// Start JSON-RPC HTTP server listening on port 7000 for POST requests to '/api'
val serverBuilder = Default.serverAsync(7000, "/api")
val server = serverBuilder(_.bind(api))
```

**Client**

```scala
// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Configure JSON-RPC to pass arguments by position instead of by name
val rpcProtocol = Default.protocol[Default.ClientContext].namedArguments(false)

// Setup custom JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val clientTransport = Default.clientTransportAsync(new URI("http://localhost:7000/api"))
val client = Client.protocol(rpcProtocol).transport(clientTransport)

// Call the remote API function
val remoteApi = client.bind[ClientApi]
println(Await.result(
  remoteApi.hello("world", 1),
  Duration.Inf
))
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```


## Integration

### [Effect system](../../examples/project/src/main/scala/examples/integration/EffectSystem.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@",
  "org.automorph" %% "automorph-zio" % "@PROJECT_VERSION@",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % "3.3.9"
)
```

**Imports**

```scala
import automorph.Default
import automorph.system.ZioSystem
import java.net.URI
import zio.{Task, Unsafe, ZIO}
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
val effectSystem = ZioSystem[Any]()

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val serverBuilder = Default.serverBuilder(effectSystem, 7000, "/api")
val server = serverBuilder(_.bind(api))
```

**Client**

```scala
// Define client view of the remote API
trait ClientApi {
  def hello(some: String, n: Int): Task[String]
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.client(effectSystem, new URI("http://localhost:7000/api"))

// Define a helper function to run ZIO tasks
def run[T](effect: Task[T]): T =
  Unsafe.unsafe { implicit unsafe =>
    ZioSystem.defaultRuntime.unsafe.run(effect).toEither.swap.map(_.getCause).swap.toTry.get
  }

// Call the remote APi function via proxy
val remoteApi = client.bind[ClientApi]
println(run(
  remoteApi.hello("world", 1)
))
```

**Cleanup**

```scala
  // Close the client
run(client.close())

// Stop the server
run(server.close())
```

### [Message codec](../../examples/project/src/main/scala/examples/integration/MessageCodec.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@",
  "org.automorph" %% "automorph-upickle" % "@PROJECT_VERSION@"
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
val messageCodec = UpickleMessagePackCodec[UpickleMessagePackCustom]()

// Provide custom data type serialization and deserialization logic
import messageCodec.custom.*
implicit def recordRw: codec.custom.ReadWriter[Record] = codec.custom.macroRW[Record]
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
val serverProtocol = Default.protocol[UpickleMessagePackCodec.Node, codec.type, Default.ServerContext](messageCodec)

// Create an effect system plugin
val effectSystem = Default.effectSystemAsync

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val handler = Handler.protocol(serverProtocol).system(effectSystem)
val server = Default.server(handler.bind(api), 7000, "/api")
```

**Client**

```scala
// Define client view of the remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[Record]
}

// Create a client RPC protocol plugin
val clientProtocol = Default.protocol[UpickleMessagePackCodec.Node, codec.type, Default.ClientContext](messageCodec)

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val clientTransport = Default.clientTransportAsync(new URI("http://localhost:7000/api"))
val client = Client(clientProtocol, clientTransport)

// Call the remote API function
val remoteApi = client.bind[ClientApi]
println(Await.result(
  remoteApi.hello("world", 1),
  Duration.Inf
))
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

### [RPC protocol](../../examples/project/src/main/scala/examples/integration/RpcProtocol.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
)
```

**Imports**

```scala
import automorph.protocol.WebRpcProtocol
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

// Create a server Web-RPC protocol plugin with '/api' path prefix
val serverProtocol = WebRpcProtocol[Default.Node, Default.Codec, Default.ServerContext](
  Default.codec, "/api"
)

// Start default Web-RPC HTTP server listening on port 7000 for requests to '/api'
val handler = Handler.protocol(serverProtocol).system(Default.effectSystemAsync).bind(api)
val server = Default.server(handler, 7000, "/api")
```

**Client**

```scala
// Define client view of the remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Create a client Web-RPC protocol plugin with '/api' path prefix
val clientProtocol = WebRpcProtocol[Default.Node, Default.Codec, Default.ClientContext](
  Default.codec, "/api"
)

// Setup default Web-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val clientTransport = Default.clientTransportAsync(new URI("http://localhost:7000/api"))
val client = Client.protocol(clientProtocol).transport(clientTransport)

// Call the remote API function
val remoteApi = client.bind[ClientApi]
println(Await.result(
  remoteApi.hello("world", 1),
  Duration.Inf
))
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```


## Transport

### [Client transport](../../examples/project/src/main/scala/examples/transport/ClientTransport.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
)
```

**Imports**

```scala
import automorph.Default
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
val serverBuilder = Default.serverSync(7000, "/api")
val server = serverBuilder(_.bind(api))
```

**Client**

```scala
// Define client view of the remote API
trait ClientApi {
  def hello(some: String, n: Int): String
}

// Create standard library HTTP client message transport sending POST requests to 'http://localhost:7000/api'
val clientTransport = UrlClient(Default.effectSystemSync, new URI("http://localhost:7000/api"))

// Setup JSON-RPC HTTP client
val client = Default.client(clientTransport)

// Call the remote API function via proxy
val remoteApi = client.bind[ClientApi]
println(
  remoteApi.hello("world", 1)
)
```

**Cleanup**

```scala
// Close the client
client.close()

// Stop the server
server.close()
```

### [Server transport](../../examples/project/src/main/scala/examples/transport/ServerTransport.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
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
val server = NanoServer(handler.bind(api), 7000, "/api")
```

**Client**

```scala
// Define client view of the remote API
trait ClientApi {
  def hello(some: String, n: Int): String
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientSync(new URI("http://localhost:7000/api"))

// Call the remote API function
val remoteApi = client.bind[ClientApi]
println(
  remoteApi.hello("world", 1)
)
```

**Cleanup**

```scala
// Close the client
client.close()

// Stop the server
server.close()
```

### [Endpoint transport](../../examples/project/src/main/scala/examples/transport/EndpointTransport.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
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
server.start()
```

**Client**

```scala
// Define client view of the remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}
// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientAsync(new URI("http://localhost:7000/api"))

// Call the remote API function via proxy
val remoteApi = client.bind[ClientApi]
println(Await.result(
  remoteApi.hello("world", 1),
  Duration.Inf
))
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
server.stop()
```

### [WebSocket transport](../../examples/project/src/main/scala/examples/transport/WebSocketTransport.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
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

// Start JSON-RPC HTTP & WebSocket server listening on port 7000 for requests to '/api'
val serverBuilder = Default.serverBuilderAsync(7000, "/api")
val server = serverBuilder(_.bind(api))
```

**Client**

```scala
// Define client view of the remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}
// Setup JSON-RPC HTTP client sending POST requests to 'ws://localhost:7000/api'
val client = Default.clientAsync(new URI("ws://localhost:7000/api"))

// Call the remote API function via proxy
val remoteApi = client.bind[ClientApi]
println(Await.result(
  remoteApi.hello("world", 1),
  Duration.Inf
))
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)
```

### [AMQP transport](../../examples/project/src/main/scala/examples/transport/AmqpTransport.scala)

**Build**

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@",
  "org.automorph" %% "automorph-rabbitmq" % "@PROJECT_VERSION@",
  "io.arivera.oss" % "embedded-rabbitmq" % "1.5.0"
)
```

**Imports**

```scala
import automorph.Default
import automorph.transport.amqp.client.RabbitMqClient
import automorph.transport.amqp.server.RabbitMqServer
import io.arivera.oss.embedded.rabbitmq.{EmbeddedRabbitMq, EmbeddedRabbitMqConfig}
import java.net.URI
import java.nio.file.Files
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.sys.process.Process
import scala.util.Try
```

**Server**

```scala
// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ServerApi()

// Start embedded RabbitMQ broker
val brokerConfig = new EmbeddedRabbitMqConfig.Builder().port(7000)
  .rabbitMqServerInitializationTimeoutInMillis(30000).build()
val broker = new EmbeddedRabbitMq(brokerConfig)
broker.start()
broker -> brokerConfig

// Start RabbitMQ AMQP server consuming requests from the 'api' queue
val handler = Default.handlerAsync[RabbitMqServer.Context]
val server = RabbitMqServer(handler.bind(api), new URI("amqp://localhost"), Seq("api"))
```

**Client**

```scala
// Define client view of the remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Create RabbitMQ AMQP client message transport publishing requests to the 'api' queue
val clientTransport = RabbitMqClient(new URI("amqp://localhost"), "api", Default.effectSystemAsync)

// Setup JSON-RPC HTTP client
val client = Default.client(clientTransport)

// Call the remote API function
val remoteApi = client.bind[ClientApi]
println(Await.result(
  remoteApi.hello("world", 1),
  Duration.Inf
))
```

**Cleanup**

```scala
// Close the client
Await.result(client.close(), Duration.Inf)

// Stop the server
Await.result(server.close(), Duration.Inf)

// Stop embedded RabbitMQ broker
broker.stop()
val brokerDirectory = brokerConfig.getExtractionFolder.toPath.resolve(brokerConfig.getVersion.getExtractionFolder)
Files.walk(brokerDirectory).iterator().asScala.toSeq.reverse.foreach(_.toFile.delete())
}
```

# Examples

## [Synchronous]

* [Source](/test/examples/src/test/scala/test/examples/Synchronous.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "io.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
// Define an API type and create API instance
class Api {
  def hello(some: String, n: Int): String = s"Hello $some $n!"
}
val api = new Api()
```

**Server**

```scala
  // Start RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer.sync(_.bind(api), 80, "/api")

// Stop the server
server.close()
```

**Client**

```scala
// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val url = new java.net.URI("http://localhost/api")
val client = automorph.DefaultHttpClient.sync(url, "POST")

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // String

// Close the client
client.close()
```

## [Asynchronous]

* [Source](/test/examples/src/test/scala/test/examples/Asynchronous.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "io.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Define an API type and create API instance
class Api {
  def hello(some: String, n: Int): Future[String] = Future.successful(s"Hello $some $n!")
}
val api = new Api()

```

**Server**

```scala
// Start RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer.async(_.bind(api), 80, "/api")

// Stop the server
server.close()
```

**Client**

```scala
// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val url = new java.net.URI("http://localhost/api")
val client = automorph.DefaultHttpClient.async(url, "POST")

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // Future[String]

// Close the client
client.close()
```

**Dynamic Client**

```scala
// Call a remote API method dynamically passing the arguments by name
val hello = client.method("hello")
hello.args("some" -> "world", "n" -> 1).call[String] // Future[String]

// Call a remote API method dynamically passing the arguments by position
hello.positional.args("world", 1).call[String] // Future[String]

// Notify a remote API method dynamically passing the arguments by name
hello.args("some" -> "world", "n" -> 1).tell // Future[Unit]

// Notify a remote API method dynamically passing the arguments by position
hello.positional.args("world", 1).tell // Future[Unit]
```

## [Request metadata]

* [Source](/test/examples/src/test/scala/test/examples/RequestMetadata.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "io.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
  // Define server API type and create API instance
class ServerApi {

  import automorph.DefaultHttpServer.Context

  // Use request context provided by the server transport
  def useMetadata(message: String)(implicit context: Context): String = Seq(
    Some(message),
    context.path,
    context.header("X-Test")
  ).flatten.mkString(",")
}
val api = new ServerApi()

// Define client view of the server API
trait ClientApi {
  import automorph.DefaultHttpClient.Context

  // Supply request context used by the client transport
  def useMetadata(message: String)(implicit context: Context): String
}
```

**Server**

```scala
// Start RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer.sync(_.bind(api), 80, "/api")

// Stop the server
server.close()
```

**Client**

```scala
// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val url = new java.net.URI("http://localhost/api")
val client = automorph.DefaultHttpClient.sync(url, "POST")

// Create client request context specifying HTTP request meta-data
val apiProxy = client.bind[ClientApi] // Api
val context = client.context
  .queryParameters("test" -> "value")
  .headers("X-Test" -> "value", "Cache-Control" -> "no-cache")
  .cookies("Test" -> "value")
  .authorizationBearer("value")

// Call the remote API method via proxy with request context supplied directly
apiProxy.useMetadata("test")(context) // String

// Call the remote API method dynamically with request context supplied directly
client.method("useMetadata").args("message" -> "test").call[String] // String

// Call the remote API method via proxy with request context supplied implictly
implicit lazy val implicitContext: automorph.DefaultHttpClient.Context = context
apiProxy.useMetadata("test") // String

// Call the remote API method dynamically with request context supplied implictly
client.method("useMetadata").args("message" -> "test").call[String] // String

// Close the client
client.close()
```

## [Method mapping]

* [Source](/test/examples/src/test/scala/test/examples/MethodMapping.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "io.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
// Define an API type and create API instance
class Api {
  // Exposed as 'test.multiParams'
  def multiParams(add: Boolean)(n: Double): Double = if (add) n + 1 else n - 1

  // Exposed as 'original' and 'aliased'
  def original(value: Option[String]): String = value.getOrElse("")

  // Not exposed
  def omitted(): String = ""
}
val api = new Api()

// Define client view of the server API
trait ClientApi {
  import DefaultHttpClient.Context

  // Supply requets context used by the client transport
  def requestMetaData(message: String)(implicit context: Context): Future[List[String]]
}
```

**Server**

```scala
// Customize method names
val mapMethodName = (name: String) => name match {
  case "original" => Seq("original", "aliased")
  case "omitted" => Seq()
  case other => Seq(s"test.$other")
}

// Start RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer.sync(_.bind(api, mapMethodName(_)), 80, "/api")

// Stop the server
server.close()
```

**Client**

```scala
import scala.util.Try

// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val url = new java.net.URI("http://localhost/api")
val client = automorph.DefaultHttpClient.sync(url, "POST")

// Call the remote API method via proxy
client.method("test.multiParams").args("add" -> true, "n" -> 1).call[Double] // 2
client.method("aliased").args("value" -> None).tell // ()
Try(client.method("omitted").args().call[String]) // Failure

// Close the client
client.close()
```

## [Error mapping]

* [Source](/test/examples/src/test/scala/test/examples/ErrorMapping.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "io.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
import automorph.protocol.ErrorType
import automorph.{Client, DefaultHttpClient, DefaultHttpServer, Handler}
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Define an API type and create API instance
class Api {
  def hello(some: String, n: Int): Future[String] = Future.successful(s"Hello $some $n!")
}
val api = new Api()

```

**Server**

```scala
// Customize default server error mapping
val exceptionToError = (exception: Throwable) =>
  Handler.defaultErrorMapping(exception) match {
    case ErrorType.ApplicationError if exception.isInstanceOf[SQLException] => ErrorType.InvalidRequest
    case error => error
  }

// Start RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = DefaultHttpServer.async(_.bind(api).errorMapping(exceptionToError), 80, "/api")

// Stop the server
server.close()
```

**Client**

```scala
// Customize default client error mapping
val errorToException = (code: Int, message: String) =>
  Client.defaultErrorMapping(code, message) match {
    case _: ErrorType.InvalidRequestException if message.toUpperCase.contains("SQL") => new SQLException(message)
    case exception => exception
  }

// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val url = new java.net.URI("http://localhost/api")
val client = DefaultHttpClient.async(url, "POST").errorMapping(errorToException)

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // Future[String]

// Close the client
client.close()
```

**Dynamic Client**

```scala
// Call a remote API method dynamically passing the arguments by name
val hello = client.method("hello")
hello.args("some" -> "world", "n" -> 1).call[String] // Future[String]

// Call a remote API method dynamically passing the arguments by position
hello.positional.args("world", 1).call[String] // Future[String]

// Notify a remote API method dynamically passing the arguments by name
hello.args("some" -> "world", "n" -> 1).tell // Future[Unit]

// Notify a remote API method dynamically passing the arguments by position
hello.positional.args("world", 1).tell // Future[Unit]
```

## [Effect system]

* [Source](/test/examples/src/test/scala/test/examples/EffectSystem.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "io.automorph" %% "automorph-default" % "0.0.1",
  "io.automorph" %% "automorph-zio" % "0.0.1",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % "3.3.9"
)
```

**API**

```scala
import automorph.system.ZioSystem
import automorph.{DefaultHttpClient, DefaultHttpServer}
import org.asynchttpclient.DefaultAsyncHttpClient
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.{Runtime, Task}
import zio.Runtime.default.unsafeRunTask

// Define an API type and create API instance
class Api {
  def hello(some: String, n: Int): Task[String] = Task.succeed(s"Hello $some $n!")
}
val api = new Api()
```

**Server**

```scala
// Create an effect system plugin
val system = ZioSystem[Any]()

// Start RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = DefaultHttpServer.system(system, unsafeRunTask, _.bind(api), 80, "/api")

// Stop the server
server.close()
```

**Client**

```scala
// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val url = new java.net.URI("http://localhost/api")
val backend = AsyncHttpClientZioBackend.usingClient(Runtime.default, new DefaultAsyncHttpClient())
val client = DefaultHttpClient(url, "POST", backend, system)

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // Task[String]

// Close the client
client.close()
```

## [Message format]

* [Source](/test/examples/src/test/scala/test/examples/MessageFormat.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "io.automorph" %% "automorph-default" % "0.0.1",
  "io.automorph" %% "automorph-upickle" % "0.0.1"
)
```

**API**

```scala
import automorph.format.messagepack.UpickleMessagePackFormat
import automorph.{Client, DefaultEffectSystem, DefaultHttpClientTransport, DefaultHttpServer, Handler}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Define an API type and create API instance
case class Record(values: List[String])
class Api {
  def hello(some: String, n: Int): Future[Record] = Future.successful(Record(List("Hello", some, n.toString)))
}
val api = new Api()
```

**Server**

```scala
// Create message format and custom data type serializer/deserializer
val format = UpickleMessagePackFormat()
implicit def recordRw: format.custom.ReadWriter[Record] = format.custom.macroRW

// Create an effect system plugin
val system = DefaultEffectSystem.async

// Start RPC server listening on port 80 for HTTP requests with URL path '/api'
val handler = Handler[UpickleMessagePackFormat.Node, format.type, Future, DefaultHttpServer.Context](format, system)
val server = DefaultHttpServer(handler.bind(api), identity, 80, "/api")

// Stop the server
server.close()
```

**Client**

```scala
// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val url = new java.net.URI("http://localhost/api")
val transport = DefaultHttpClientTransport.async(url, "POST")
val client = Client[UpickleMessagePackFormat.Node, format.type, Future, DefaultHttpClientTransport.Context](format, system, transport)

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // Future[String]

// Close the client
client.close()
```

## [Client message transport]

* [Source](/test/examples/src/test/scala/test/examples/ClientMessageTransport.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "io.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
import automorph.transport.http.client.HttpUrlConnectionClient
import automorph.{DefaultClient, DefaultHttpServer}

// Define an API type and create API instance
class Api {
  def hello(some: String, n: Int): String = s"Hello $some $n!"
}
val api = new Api()
```

**Server**

```scala
import automorph.system.IdentitySystem.Identity
import automorph.transport.http.server.NanoHttpdServer
import automorph.{Client, DefaultSystem, DefaultFormat, Handler}

// Start RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = DefaultHttpServer.sync(_.bind(api), 80, "/api")

// Stop the server
server.close()
```

**Client**

```scala
// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val url = new java.net.URI("http://localhost/api")
val transport = HttpUrlConnectionClient(url, "POST")
val client = DefaultClient.sync(transport)

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // String

// Close the client
client.close()
```

## [Server message transport]

* [Source](/test/examples/src/test/scala/test/examples/ServerMessageTransport.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "io.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
import automorph.transport.http.server.NanoHttpdServer
import automorph.{DefaultHandler, DefaultHttpClient}

// Define an API type and create API instance
class Api {
  def hello(some: String, n: Int): String = s"Hello $some $n!"
}
val api = new Api()
```

**Server**

```scala
// Start RPC server listening on port 80 for HTTP requests with URL path '/api'
val handler = DefaultHandler.sync[NanoHttpdServer.Context]
val server = NanoHttpdServer(handler.bind(api), identity, 80)

// Stop the server
server.close()
```

**Client**

```scala
// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val url = new java.net.URI("http://localhost/api")
val client = DefaultHttpClient.sync(url, "POST")

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // String

// Close the client
client.close()
```

## [Endpoint message transport]

* [Source](/test/examples/src/test/scala/test/examples/EndpointMessageTransport.scala)

**Dependencies**

```scala
libraryDependencies ++= Seq(
  "io.automorph" %% "automorph-default" % "0.0.1"
)
```

**API**

```scala
import io.undertow.{Handlers, Undertow}
import automorph.{DefaultHandler, DefaultHttpClient}
import automorph.transport.http.endpoint.UndertowHttpEndpoint
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Define an API type and create API instance
class Api {
  def hello(some: String, n: Int): Future[String] = Future(s"Hello $some $n!")
}
val api = new Api()
```

**Server**

```scala
// Start RPC server listening on port 80 for HTTP requests with URL path '/api'
val handler = DefaultHandler.async[UndertowHttpEndpoint.Context]
val endpoint = UndertowHttpEndpoint(handler.bind(api), identity)
val pathHandler = Handlers.path().addPrefixPath("/api", endpoint)
val server = Undertow.builder()
  .addHttpListener(80, "0.0.0.0")
  .setHandler(pathHandler)
  .build()

// Stop the server
server.stop()
```

**Client**

```scala
// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val url = new java.net.URI("http://localhost/api")
val client = DefaultHttpClient.async(url, "POST")

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // Future[String]

// Close the client
client.close()
```

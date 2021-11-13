# Quickstart

Expose and call a remote JSON-RPC API over HTTP.

## [Source](../../examples/project/src/main/scala/examples/QuickStart.scala)

### Build

Add the following to your `build.sbt` file:

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@VERSION@"
)
```

### Server

Expose the API instance for remote calls using JSON-RPC over HTTP(S).

```scala
import automorph.Default
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ServerApi()

// Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val createServer = Default.serverAsync(7000, "/api")
val server = createServer(_.bind(api))

// Stop the server
Await.result(server.close(), Duration.Inf)
```

### Static client

Call the remote API instance via proxy created from API type using JSON-RPC over HTTP(S).

```scala
import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientAsync(new URI("http://localhost:7000/api"))

// Call the remote API function statically
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Future[String]

// Close the client
Await.result(client.close(), Duration.Inf)
```

### Dynamic client

Call the remote API dynamically without API type definition using JSON-RPC over HTTP(S).

```scala
import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

// Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientAsync(new URI("http://localhost:7000/api"))

// Call the remote API function dynamically
client.call[String]("hello").args("what" -> "world", "n" -> 1) // Future[String]

// Close the client
client.close()
```

## [Project](https://github.com/martin-ockajak/automorph/examples/project)

### Download

Clone the repository and enter example project directory:

```shell
git clone https://github.com/martin-ockajak/automorph && cd automorph/examples/project
```

### Try

Run the application:

```shell
sbt run
```

### Customize

Make changes:

```shell
edit src/main/scala/examples/QuickStart.scala
```

### Explore

Review additional examples:
```shell
find src/test
```


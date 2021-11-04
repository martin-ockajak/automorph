# Quickstart

Exposing and invoking a JSON-RPC API using HTTP as transport protocol.

* [Scaladoc](../api/automorph/index.html)
* [Artifacts](https://mvnrepository.com/artifact/org.automorph/automorph)
* [Source](/test/examples/src/test/scala/test/examples/Synchronous.scala)

## Build

Add the following to your `build.sbt` file:

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

## Server

Expose the API instance for remote calls using JSON-RPC over HTTP(S).

```scala
import automorph.Default
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ServerApi()

// Start default JSON-RPC HTTP server listening on port 7000 for requests to '/api'
val createServer = Default.serverAsync(7000, "/api")
val server = createServer(_.bind(api))

// Stop the server
server.close()
```

## Static Client

Call the remote API instance via proxy created from API type using JSON-RPC over HTTP(S).

```scala
import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Define client view of a remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientAsync(new URI("http://localhost:7000/api"))

// Call the remote API function statically
val remoteApi = client.bind[ClientApi] // ClientApi
remoteApi.hello("world", 1) // Future[String]

// Close the client
client.close()
```

## Dynamic Client

Call the remote API dynamically without API type definition using JSON-RPC over HTTP(S).

```scala
import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
val client = Default.clientAsync(new URI("http://localhost:7000/api"))

// Call the remote API function dynamically
client.call[String]("hello").args("what" -> "world", "n" -> 1) // Future[String]

// Close the client
client.close()
```

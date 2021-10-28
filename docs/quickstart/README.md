# Quickstart

Exposing and invoking a JSON-RPC API using HTTP as transport protocol.

* [Scaladoc](../api/automorph/index.html)
* [Artifacts](https://mvnrepository.com/artifact/org.automorph/automorph)

## Build

Add the following to your `build.sbt` file:

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

## API

Take an asynchronous API:

```scala
import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Define an API
trait Api {
  def hello(some: String, n: Int): Future[String]
}

// Create the API instance
class ApiImpl extends Api {
  override def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ApiImpl()

```

## Server

Expose the API instance for remote calls using JSON-RPC over HTTP(S).

```scala
// Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
val server = Default.serverAsync(_.bind(api), 80, "/api")

// Stop the server
server.close()
```

## Static Client

Call the remote API instance via proxy created from API type using JSON-RPC over HTTP(S).

```scala
// Initialize STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
val client = Default.clientAsync(new URI("http://localhost/api"), "POST")

// Call the remote API function statically
val remoteApi = client.bind[Api] // Api
remoteApi.hello("world", 1) // Future[String]
```

## Dynamic Client

Call the remote API dynamically without API type definition using JSON-RPC over HTTP(S).

```scala
// Call the remote API function dynamically
client.function("hello")
  .args("what" -> "world", "n" -> 1)
  .call[String] // Future[String]

// Close the client
client.close()
```

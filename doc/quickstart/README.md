# Quickstart

Exposing and invoking a JSON-RPC API using HTTP as transport protocol.

* [Scaladoc](https://www.javadoc.io/doc/org.automorph/automorph-core_3.0.0/latest/)

## Build

Add the following to your `build.sbt` file:

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "0.0.1"
)
```

## API

Take an existing asynchronous API:

```scala
import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Define an API
trait Api {
  def hello(what: String, n: Int): Future[String]
}

// Create the API implementation
class ApiImpl extends Api {
  override def hello(what: String, n: Int): Future[String] = Future(s"Hello $n $what!")
}
val api = new ApiImpl()

```

Expose the API instance for remote calls using JSON-RPC over HTTP(S).

## Server

```scala
// Start RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = Default.asyncHttpServer(_.bind(api), 80, "/api")

// Stop the server
server.close()
```

## Static Client

Call the remote API instance via proxy created from API type using JSON-RPC over HTTP(S).

```scala
// Create RPC client sending HTTP POST requests to 'http://localhost/api'
val client = Default.asyncHttpClient(new URI("http://localhost/api"), "POST")

// Call the remote API function via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 3) // Future[String]
```

## Dynamic Client

Call the remote API dynamically without API type definition using JSON-RPC over HTTP(S).

```scala
// Call a remote API function dynamically
val hello = client.function("hello")
hello.args("what" -> "world", "n" -> 1).call[String] // Future[String]

// Notify a remote API function dynamically
hello.args("what" -> "world", "n" -> 1).tell // Future[Unit]

// Close the client
client.close()
```

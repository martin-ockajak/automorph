# Quickstart

Exposing and invoking a JSON-RPC API using HTTP as transport protocol.

* [Scaladoc](https://www.javadoc.io/doc/org.automorph/automorph-core_2.13/latest/)

## Build

Add the following to your `build.sbt` file:

```scala
libraryDependencies += "org.automorph" %% "automorph-default" % "0.0.1"
```

## API

Take an existing asynchronous API:

```scala
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Define an API type and create API instance
class Api {
  def hello(some: String, n: Int): Future[String] = Future(s"Hello $some $n!")
}
val api = new Api()

```

Expose the API via JSON-RPC over HTTP(S).

## Server

```scala
// Start RPC server listening on port 80 for HTTP requests with URL path '/api'
val server = automorph.DefaultHttpServer.async(_.bind(api), 80, "/api")

// Stop the server
server.close()
```

## Static Client

Invoke the API via JSON-RPC over HTTP(S).

```scala
// Create RPC client for sending HTTP POST requests to 'http://localhost/api'
val url = new java.net.URI("http://localhost/api")
val client = automorph.DefaultHttpClient.async(url, "POST")

// Call the remote API method via proxy
val apiProxy = client.bind[Api] // Api
apiProxy.hello("world", 1) // Future[String]
```

## Dynamic Client

Invoke the API dynamically without definition via JSON-RPC over HTTP(S).

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

// Close the client
client.close()
```

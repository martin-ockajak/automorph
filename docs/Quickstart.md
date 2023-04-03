---
sidebar_position: 2
---

# Quickstart

Expose and call a remote JSON-RPC API over HTTP.

## [Existing project](../../examples/project/src/main/scala/examples/Quickstart.scala)

### Build

Add the following dependency to your build configuration:

#### SBT

```scala
libraryDependencies ++= Seq(
  "org.automorph" %% "automorph-default" % "@PROJECT_VERSION@"
)
```

#### Gradle

```yaml
implementation group: 'org.automorph', name: 'automorph-default_3', version: '@PROJECT_VERSION@'
```

### Server

Expose the API instance for remote calls using JSON-RPC over HTTP(S).

```scala
import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

// Define a helper function to evaluate Futures
def run[T](effect: Future[T]): T = Await.result(effect, Duration.Inf)

// Create server API instance
class ServerApi {
  def hello(some: String, n: Int): Future[String] =
    Future(s"Hello $some $n!")
}
val api = new ServerApi()

// Start JSON-RPC HTTP & WebSocket server listening on port 7000 for requests to '/api'
val server = run(
  Default.serverAsync(7000, "/api").bind(api).init()
)

// Stop the RPC server
run(server.close())
```

### Static client

Call the remote API instance via proxy created from API type using JSON-RPC over HTTP(S).

```scala
import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

// Define a helper function to evaluate Futures
def run[T](effect: Future[T]): T = Await.result(effect, Duration.Inf)

// Define client view of the remote API
trait ClientApi {
  def hello(some: String, n: Int): Future[String]
}

// Setup JSON-RPC HTTP & WebSocket client sending POST requests to 'http://localhost:7000/api'
val client = run(
  Default.clientAsync(new URI("http://localhost:7000/api")).init()
)

// Call the remote API function statically
val remoteApi = client.bind[ClientApi]
println(run(
  remoteApi.hello("world", 1)
))

// Close the RPC client
run(client.close())
```

### Dynamic client

Call the remote API dynamically without API type definition using JSON-RPC over HTTP(S).

```scala
import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

// Define a helper function to evaluate Futures
def run[T](effect: Future[T]): T = Await.result(effect, Duration.Inf)

// Setup JSON-RPC HTTP & WebSocket client sending POST requests to 'http://localhost:7000/api'
val client = run(
  Default.clientAsync(new URI("http://localhost:7000/api")).init()
)

// Call the remote API function dynamically
println(run(
  client.call[String]("hello")("some" -> "world", "n" -> 1)
))

// Close the RPC client
run(client.close())
```

## [Example project](https://github.com/martin-ockajak/automorph/examples/project)

### Download

Clone the repository and enter the example project directory:

```shell
git clone https://github.com/martin-ockajak/automorph
cd automorph/examples/project
```

### Try

Run any of the examples:

```shell
sbt run
```

### Adjust

- Remove unused examples in and build dependencies

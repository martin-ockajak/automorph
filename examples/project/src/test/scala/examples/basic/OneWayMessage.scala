package examples.basic

import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object OneWayMessage extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new ServerApi()

  // Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
  val createServer = Default.serverAsync(7000, "/api")
  val server = createServer(_.bind(api))

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): Future[String]
  }
  // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
  val client = Default.clientAsync(new URI("http://localhost:7000/api"))

  // Message the remote API function dynamically without expecting a response
  client.message("hello").args("some" -> "world", "n" -> 1) // Future[Unit]

  // Close the client
  Await.result(client.close(), Duration.Inf)

  // Stop the server
  Await.result(server.close(), Duration.Inf)
}

class OneWayMessage extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" in {
      OneWayMessage.main(Array())
    }
  }
}

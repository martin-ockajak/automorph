package test.examples

import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DynamicNotification extends App {

  // Define an API and create its instance
  class Api {
    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new Api()

  // Start default JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val createServer = Default.serverAsync(80, "/api")
  val server = createServer(_.bind(api))

  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.clientAsync(new URI("http://localhost/api"))

  // Notify the remote API function dynamically without expecting a response
  client.notify("hello").args("some" -> "world", "n" -> 1) // Future[Unit]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class DynamicNotification extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      Asynchronous.main(Array())
    }
  }
}

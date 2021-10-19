package test.examples

import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DynamicNotification extends App {

  // Define an API and create its instance
  class Api {
    def hello(what: String, n: Int): Future[String] = Future(s"Hello $n $what!")
  }
  val api = new Api()

  // Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val server = Default.asyncServer(_.bind(api), 80, "/api")

  // Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.asyncClient(new URI("http://localhost/api"), "POST")

  // Call the remote API function dynamically
  val remoteHello = client.function("hello")
  remoteHello.args("what" -> "world", "n" -> 3).call[String] // Future[String]

  // Notify the remote API function dynamically without expecting a response
  remoteHello.args("what" -> "world", "n" -> 3).tell // Future[Unit]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class DynamicNotification extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      Asynchronous.main(Array())
    }
  }
}

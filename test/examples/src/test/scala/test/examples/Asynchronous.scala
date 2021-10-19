package test.examples

import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Asynchronous extends App {

  // Define an API and create its instance
  class Api {
    def hello(some: String, n: Int): Future[String] = Future(s"Hello $some $n!")
  }
  val api = new Api()

  // Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val server = Default.asyncServer(_.bind(api), 80, "/api")

  // Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.asyncClient(new URI("http://localhost/api"), "POST")

  // Call the remote API function via proxy
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 3) // Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class Asynchronous extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      Asynchronous.main(Array())
    }
  }
}

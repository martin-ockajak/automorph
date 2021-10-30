package test.examples

import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object QuickStart extends App {

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

  // Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val createServer = Default.serverAsync(80, "/api")
  val server = createServer(_.bind(api))

  // Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.clientAsync(new URI("http://localhost/api"), "POST")

  // Call the remote API function statically
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 1) // Future[String]

  // Call the remote API function dynamically
  client.call[String]("hello").args("what" -> "world", "n" -> 1) // Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class QuickStart extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      QuickStart.main(Array())
    }
  }
}
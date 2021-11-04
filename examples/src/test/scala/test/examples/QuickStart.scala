package test.examples

import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object QuickStart extends App {

  // Create the API instance
  class ServerApi {
    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new ServerApi()

  // Start default JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val createServer = Default.serverAsync(80, "/api")
  val server = createServer(_.bind(api))

  // Define an API
  trait ClientApi {
    def hello(some: String, n: Int): Future[String]
  }

  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.clientAsync(new URI("http://localhost/api"))

  // Call the remote API function statically
  val remoteApi = client.bind[ClientApi] // ClientApi
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

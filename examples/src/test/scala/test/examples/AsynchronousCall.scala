package test.examples

import automorph.Default
import automorph.transport.http.HttpMethod
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object AsynchronousCall extends App {

  // Define an API and create its instance
  class Api {
    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new Api()

  // Start default JSON-RPC HTTP server listening on port 80 for PUT requests to '/api'
  val createServer = Default.serverAsync(80, "/api", Seq(HttpMethod.Put))
  val server = createServer(_.bind(api))

  // Setup default JSON-RPC HTTP client sending PUT requests to 'http://localhost/api'
  val client = Default.clientAsync(new URI("http://localhost/api"), HttpMethod.Put)

  // Call the remote API function
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 1) // Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class AsynchronousCall extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      AsynchronousCall.main(Array())
    }
  }
}

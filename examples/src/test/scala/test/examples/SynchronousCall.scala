package test.examples

import automorph.Default
import automorph.transport.http.HttpMethod
import java.net.URI

object SynchronousCall extends App {

  // Define an API type and create its instance
  class Api {
    def hello(some: String, n: Int): String =
      s"Hello $some $n!"
  }
  val api = new Api()

  // Start default JSON-RPC HTTP server listening on port 80 for POST requests to '/api'
  val createServer = Default.serverSync(80, "/api", Seq(HttpMethod.Post))
  val server = createServer(_.bind(api))

  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.clientSync(new URI("http://localhost/api"), HttpMethod.Post)

  // Call the remote API function
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 1) // String

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class SynchronousCall extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      SynchronousCall.main(Array())
    }
  }
}

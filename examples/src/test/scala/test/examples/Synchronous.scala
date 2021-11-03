package test.examples

import automorph.Default
import java.net.URI

object Synchronous extends App {

  // Define an API type and create its instance
  class Api {
    def hello(some: String, n: Int): String =
      s"Hello $some $n!"
  }
  val api = new Api()

  // Start default JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val createServer = Default.serverSync(80, "/api")
  val server = createServer(_.bind(api))

  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.clientSync(new URI("http://localhost/api"))

  // Call the remote API function
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 1) // String

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class Synchronous extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      Synchronous.main(Array())
    }
  }
}

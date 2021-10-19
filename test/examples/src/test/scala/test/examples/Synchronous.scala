package test.examples

import automorph.Default
import java.net.URI

object Synchronous extends App {

  // Define an API type and create its instance
  class Api {
    def hello(some: String, n: Int): String = s"Hello $some $n!"
  }
  val api = new Api()

  // Start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = Default.syncHttpServer(_.bind(api), 80, "/api")

  // Create JSON-RPC client sending HTTP POST requests to 'http://localhost/api'
  val client = Default.syncHttpClient(new URI("http://localhost/api"), "POST")

  // Call the remote API function
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 1) // String

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class Synchronous extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      Synchronous.main(Array())
    }
  }
}

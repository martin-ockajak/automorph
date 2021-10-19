package test.example

import automorph.Default
import automorph.transport.http.server.NanoHttpdServer
import java.net.URI

object ServerMessageTransport extends App {

  // Define an API type and create its instance
  class Api {
    def hello(some: String, n: Int): String = s"Hello $some $n!"
  }
  val api = new Api()

  // Start NanoHTTPD JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val handler = Default.syncHandler[NanoHttpdServer.Context]
  val server = NanoHttpdServer(handler.bind(api), identity, 80)

  // Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.syncClient(new URI("http://localhost/api"), "POST")

  // Call the remote API function
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 1) // String

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class ServerMessageTransport extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      ServerMessageTransport.main(Array())
    }
  }
}

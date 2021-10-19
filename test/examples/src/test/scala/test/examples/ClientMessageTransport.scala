package test.examples

import automorph.Default
import automorph.system.IdentitySystem
import automorph.transport.http.client.HttpUrlConnectionClient
import java.net.URI

object ClientMessageTransport extends App {

  // Define an API and create its instance
  class Api {
    def hello(some: String, n: Int): String = s"Hello $some $n!"
  }
  val api = new Api()

  // Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val server = Default.syncServer(_.bind(api), 80, "/api")

  // Create HttpUrlConnection HTTP client message transport
  val transport = HttpUrlConnectionClient(new URI("http://localhost/api"), "POST", IdentitySystem())

  // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.client(transport)

  // Call the remote API function via proxy
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 1) // : String

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class ClientMessageTransport extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      ClientMessageTransport.main(Array())
    }
  }
}

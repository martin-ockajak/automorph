package test.examples.select

import automorph.Default
import automorph.transport.http.server.NanoServer
import java.net.URI

object ServerTransportSelection extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): String =
      s"Hello $some $n!"
  }
  val api = new ServerApi()

  // Start NanoHTTPD JSON-RPC HTTP server listening on port 8080 for requests to '/api'
  val handler = Default.handlerSync[NanoServer.Context]
  val createServer = NanoServer.create(handler.bind(api), 8080)
  val server = createServer(identity)

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): String
  }

  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.clientSync(new URI("http://localhost/api"))

  // Call the remote API function
  val remoteApi = client.bind[ClientApi] // ClientApi
  remoteApi.hello("world", 1) // String

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class ServerTransportSelection extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      ServerTransportSelection.main(Array())
    }
  }
}

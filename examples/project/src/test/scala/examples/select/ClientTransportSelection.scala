package examples.select

import automorph.Default
import automorph.system.IdentitySystem
import automorph.transport.http.client.UrlClient
import java.net.URI

object ClientTransportSelection extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): String =
      s"Hello $some $n!"
  }
  val api = new ServerApi()

  // Start JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val createServer = Default.serverSync(80, "/api")
  val server = createServer(_.bind(api))

  // Create HttpUrlConnection HTTP client message transport
  val transport = UrlClient(IdentitySystem(), new URI("http://localhost:7000/api"))

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): String
  }
  // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
  val client = Default.client(transport)

  // Call the remote API function via proxy
  val remoteApi = client.bind[ClientApi] // ClientApi
  remoteApi.hello("world", 1) // : String

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class ClientTransportSelection extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      ClientTransportSelection.main(Array())
    }
  }
}

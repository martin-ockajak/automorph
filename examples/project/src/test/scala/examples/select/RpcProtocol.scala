package examples.select

import automorph.protocol.WebRpcProtocol
import automorph.{Client, Default, Handler}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object RpcProtocol extends App {

  // Create server API instance
  class ServerApi {

    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new ServerApi()

  // Create a server Web-RPC protocol plugin with '/api' path prefix
  val serverProtocol = WebRpcProtocol(Default.codec, "/api/" ).context[Default.ServerContext]

  // Start default Web-RPC HTTP server listening on port 7000 for requests to '/api'
  val handler = Handler.protocol(serverProtocol).system(Default.systemAsync)
  val server = Default.server(handler, 7000, "/api")

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): Future[String]
  }

  // Create a client Web-RPC protocol plugin with '/api' path prefix
  val clientProtocol = WebRpcProtocol(Default.codec, "/api/").context[Default.ClientContext]

  // Setup default Web-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
  val transport = Default.clientTransportAsync(new URI("http://localhost:7000/api"))
  val client = Client.protocol(clientProtocol).transport(transport)

  // Call the remote API function
  val remoteApi = client.bind[ClientApi] // ClientApi
  remoteApi.hello("world", 1) // Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class RpcProtocol extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      RpcProtocol.main(Array())
    }
  }
}

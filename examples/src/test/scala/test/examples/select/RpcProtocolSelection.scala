package test.examples.select

import automorph.protocol.RestRpcProtocol
import automorph.{Client, Default, Handler}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object RpcProtocolSelection extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new ServerApi()

  // Create a server REST-RPC protocol plugin with '/api' path prefix
  val serverProtocol = RestRpcProtocol[Default.Node, Default.Codec, Default.ServerContext](
    Default.codec,
    "/api/"
  )

  // Start default REST-RPC HTTP server listening on port 7000 for requests to '/api'
  val system = Default.systemAsync
  val handler = Handler.protocol(serverProtocol).system(system)
  val server = Default.server(handler, 7000, "/api")

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): Future[String]
  }

  // Create a client REST-RPC protocol plugin with '/api' path prefix
  val clientProtocol = RestRpcProtocol[Default.Node, Default.Codec, Default.ClientContext](
    Default.codec,
    "/api/"
  )

  // Setup default REST-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
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

class RpcProtocolSelection extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      RpcProtocolSelection.main(Array())
    }
  }
}

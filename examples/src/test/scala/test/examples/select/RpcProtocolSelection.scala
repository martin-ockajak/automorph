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

  // Create REST-RPC protocol plugin
  val protocol = RestRpcProtocol[Default.Node, Default.Codec](Default.codec)

  // Start default REST-RPC HTTP server listening on port 7000 for requests to '/api'
  val system = Default.systemAsync
  val handler = Handler.protocol(protocol).system(system).context[Default.ServerContext]
  val server = Default.server(handler, 7000, "/api")

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): Future[String]
  }
  // Setup default REST-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
  val transport = Default.clientTransportAsync(new URI("http://localhost:7000/api"))
  val client = Client.protocol(protocol).transport(transport)

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

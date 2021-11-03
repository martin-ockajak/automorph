package test.examples

import automorph.protocol.RestRpcProtocol
import automorph.{Client, Default, Handler}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object RpcProtocol extends App {

  // Define an API type and create its instance
  class Api {
    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new Api()

  // Create REST-RPC protocol plugin
  val protocol = RestRpcProtocol[Default.Node, Default.Codec](Default.codec)

  // Start default REST-RPC HTTP server listening on port 80 for requests to '/api'
  val system = Default.systemAsync
  val handler = Handler.protocol(protocol).system(system).context[Default.ServerContext]
  val server = Default.server(handler, 80, "/api")

  // Setup default REST-RPC HTTP client sending POST requests to 'http://localhost/api'
  val transport = Default.clientTransportAsync(new URI("http://localhost/api"), "POST")
  val client = Client.protocol(protocol).transport(transport)

  // Call the remote API function
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 1) // Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}


class RpcProtocol extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      ClientErrorMapping.main(Array())
    }
  }
}

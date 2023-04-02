package examples.transport

import automorph.{Default, RpcServer}
import automorph.transport.http.server.NanoServer
import java.net.URI

object ServerTransport {
  @scala.annotation.nowarn
  def main(arguments: Array[String]): Unit = {

    // Create server API instance
    class ServerApi {
      def hello(some: String, n: Int): String =
        s"Hello $some $n!"
    }
    val api = new ServerApi

    // Create NanoHTTPD HTTP & WebSocket server transport listening on port 7000 for requests to '/api'
    val serverTransport = NanoServer(Default.effectSystemSync, 7000, "/api")

    // Start JSON-RPC HTTP & WebSocket server
    val server = RpcServer.transport(serverTransport).rpcProtocol(Default.rpcProtocol).bind(api).init()

    // Define client view of the remote API
    trait ClientApi {
      def hello(some: String, n: Int): String
    }

    // Setup JSON-RPC HTTP & WebSocket client sending POST requests to 'http://localhost:7000/api'
    val client = Default.clientSync(new URI("http://localhost:7000/api"))

    // Call the remote API function
    val remoteApi = client.bind[ClientApi]
    println(
      remoteApi.hello("world", 1)
    )

    // Close the RPC client
    client.close()

    // Stop the RPC server
    server.close()
  }
}

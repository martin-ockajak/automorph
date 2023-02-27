package examples.select

import automorph.Default
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
    val api = new ServerApi()

    // Start NanoHTTPD JSON-RPC HTTP server listening on port 7000 for requests to '/api'
    val handler = Default.handlerSync[NanoServer.Context]
    val server = NanoServer[Default.SyncEffect](handler.bind(api), identity, 7000, "/api")

    // Define client view of the remote API
    trait ClientApi {
      def hello(some: String, n: Int): String
    }

    // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
    val client = Default.clientSync(new URI("http://localhost:7000/api"))

    // Call the remote API function
    val remoteApi = client.bind[ClientApi]
    println(
      remoteApi.hello("world", 1)
    )

    // Close the client
    client.close()

    // Stop the server
    server.close()
  }
}

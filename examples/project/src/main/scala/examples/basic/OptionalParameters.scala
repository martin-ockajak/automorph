package examples.basic

import automorph.Default
import java.net.URI

private[examples] object OptionalParameters {
  @scala.annotation.nowarn
  def main(arguments: Array[String]): Unit = {

    // Create server API instance
    class ServerApi {
      def hello(some: String, n: Option[Int]): String =
        s"Hello $some ${n.getOrElse(0)}!"

      def hi(some: Option[String])(n: Int): String =
        s"Hi ${some.getOrElse("all")} $n!"
    }
    val api = new ServerApi()

    // Start JSON-RPC HTTP server listening on port 7000 for POST requests to '/api'
    val server = Default.serverSync(7000, "/api").bind(api).init()

    // Define client view of the remote API
    trait ClientApi {
      def hello(some: String): String
    }
    // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
    val client = Default.clientSync(new URI("http://localhost:7000/api")).init()

    // Call the remote API function statically
    val remoteApi = client.bind[ClientApi]
    println(
      remoteApi.hello("world")
    )

    // Call the remote API function dynamically
    client.call[String]("hi").args("n" -> 1) // String

    // Close the client
    client.close()

    // Stop the server
    server.close()
  }
}

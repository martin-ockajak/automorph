package examples.special

import automorph.{Client, Default}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

private[examples] object PositionalArguments {
  @scala.annotation.nowarn
  def main(arguments: Array[String]): Unit = {

    // Define a helper function to evaluate Futures
    def run[T](effect: Future[T]): T = Await.result(effect, Duration.Inf)

    // Create server API instance
    class ServerApi {
      def hello(some: String, n: Int): Future[String] =
        Future(s"Hello $some $n!")
    }
    val api = new ServerApi()

    // Start JSON-RPC HTTP server listening on port 7000 for POST requests to '/api'
    val server = run(
      Default.serverAsync(7000, "/api").bind(api).init(),

    )

    // Define client view of the remote API
    trait ClientApi {
      def hello(some: String, n: Int): Future[String]
    }

    // Configure JSON-RPC to pass arguments by position instead of by name
    val rpcProtocol = Default.rpcProtocol[Default.ClientContext].namedArguments(false)

    // Create HTTP client transport sending POST requests to 'http://localhost:7000/api'
    val clientTransport = Default.clientTransport(Default.effectSystemAsync, new URI("http://localhost:7000/api"))

    // Setup  JSON-RPC HTTP client
    val client = run(
      Client.transport(clientTransport).rpcProtocol(rpcProtocol).init()
    )

    // Call the remote API function
    val remoteApi = client.bind[ClientApi]
    println(run(
      remoteApi.hello("world", 1),
    ))

    // Close the client
    run(client.close())

    // Stop the server
    run(server.close())
  }
}

package examples.transport

import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

private[examples] object WebSocketTransport {
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

    // Start JSON-RPC HTTP & WebSocket server listening on port 7000 for requests to '/api'
    val server = run(
      Default.serverAsync(7000, "/api").bind(api).init()
    )

    // Define client view of the remote API
    trait ClientApi {
      def hello(some: String, n: Int): Future[String]
    }
    // Setup JSON-RPC HTTP client sending POST requests to 'ws://localhost:7000/api'
    val client = Default.clientAsync(new URI("ws://localhost:7000/api"))

    // Call the remote API function via proxy
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

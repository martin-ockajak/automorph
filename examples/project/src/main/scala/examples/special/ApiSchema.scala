package examples.special

import automorph.Default
import automorph.protocol.JsonRpcProtocol
import automorph.schema.{OpenApi, OpenRpc}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

private[examples] object ApiSchema {
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

    // Start JSON-RPC HTTP & WebSocket server listening on port 7000 for POST requests to '/api'
    val server = run(
      Default.serverAsync(7000, "/api").bind(api).init()
    )

    // Setup JSON-RPC HTTP & WebSocket client sending POST requests to 'http://localhost:7000/api'
    val client = run(
      Default.clientAsync(new URI("http://localhost:7000/api")).init()
    )

    // Retrieve the remote API schema in OpenRPC format
    println(run(
      client.call[OpenRpc](JsonRpcProtocol.openRpcFunction).args()
    ).methods.map(_.name))

    // Retrieve the remote API schema in OpenAPI format
    println(run(
      client.call[OpenApi](JsonRpcProtocol.openApiFunction).args(),
    ).paths.get.keys.toList)

    // Close the client
    run(client.close())

    // Stop the server
    run(server.close())
  }
}

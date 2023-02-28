package examples.errors

import automorph.protocol.jsonrpc.ErrorType.InvalidRequest
import automorph.protocol.jsonrpc.JsonRpcException
import automorph.{Default, Handler}
import java.net.URI
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try

private[examples] object ServerErrors {
  @scala.annotation.nowarn
  def main(arguments: Array[String]): Unit = {

    // Create server API instance
    class ServerApi {
      def hello(some: String, n: Int): Future[String] =
        if (n >= 0) {
          Future.failed(new SQLException("Invalid request"))
        } else {
          Future.failed(JsonRpcException("Application error", 1))
        }
    }
    val api = new ServerApi()

    // Customize remote API server exception to RPC error mapping
    val rpcProtocol = Default.rpcProtocol[Default.ServerContext].mapException(_ match {
      case _: SQLException => InvalidRequest
      case error => Default.rpcProtocol.mapException(error)
    })

    // Start custom JSON-RPC HTTP server listening on port 7000 for requests to '/api'
    val handler = Handler.protocol(rpcProtocol).system(Default.effectSystemAsync).bind(api)
    val server = Default.server(handler, 7000, "/api")

    // Define client view of the remote API
    trait ClientApi {
      def hello(some: String, n: Int): Future[String]
    }

    // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
    val client = Default.clientAsync(new URI("http://localhost:7000/api"))

    // Call the remote API function and fail with InvalidRequestException
    val remoteApi = client.bind[ClientApi]
    println(Try(Await.result(
      remoteApi.hello("world", 1),
      Duration.Inf
    )).failed.get)

    // Call the remote API function and fail with RuntimeException
    println(Try(Await.result(
      remoteApi.hello("world", -1),
      Duration.Inf
    )).failed.get)

    // Close the client
    Await.result(client.close(), Duration.Inf)

    // Stop the server
    Await.result(server.close(), Duration.Inf)
  }
}

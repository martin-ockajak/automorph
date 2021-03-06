package examples.customize

import automorph.protocol.jsonrpc.ErrorType.InvalidRequest
import automorph.protocol.jsonrpc.JsonRpcException
import automorph.{Default, Handler}
import java.net.URI
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object ServerProtocolErrors extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): Future[String] =
      if (n >= 0) {
        Future.failed(SQLException("Data error"))
      } else {
        Future.failed(JsonRpcException("Other error", 1))
      }
  }
  val api = new ServerApi()

  // Customize remote API server exception to RPC error mapping
  val protocol = Default.protocol[Default.ServerContext].mapException {
    case _: SQLException => InvalidRequest
    case e => Default.protocol.mapException(e)
  }

  // Start custom JSON-RPC HTTP server listening on port 7000 for requests to '/api'
  val handler = Handler.protocol(protocol).system(Default.systemAsync)
  val server = Default.server(handler, 7000, "/api")

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): Future[String]
  }
  // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
  val client = Default.clientAsync(new URI("http://localhost:7000/api"))

  // Call the remote API function
  val remoteApi = client.bind[ClientApi] // ClientApi
  remoteApi.hello("world", 1) // JSON-RPC invalid request error with code: -32600
  remoteApi.hello("world", -1) // JSON-RPC application error with code: 1

  // Close the client
  Await.result(client.close(), Duration.Inf)

  // Stop the server
  Await.result(server.close(), Duration.Inf)
}

class ServerProtocolErrors extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" in {
      ServerProtocolErrors.main(Array())
    }
  }
}

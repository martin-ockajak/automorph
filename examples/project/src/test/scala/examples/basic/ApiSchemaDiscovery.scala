package examples.basic

import automorph.Default
import automorph.schema.{OpenApi, OpenRpc}
import automorph.protocol.JsonRpcProtocol
import automorph.transport.http.HttpMethod
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object ApiSchemaDiscovery extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new ServerApi()

  // Start JSON-RPC HTTP server listening on port 7000 for PUT requests to '/api'
  val createServer = Default.serverAsync(7000, "/api", Seq(HttpMethod.Put))
  val server = createServer(_.bind(api))

  // Setup JSON-RPC HTTP client sending PUT requests to 'http://localhost:7000/api'
  val client = Default.clientAsync(new URI("http://localhost:7000/api"), HttpMethod.Put)

  // Retrieve the remote API schema in OpenRPC format
  println(Await.result(
    client.call[OpenRpc](JsonRpcProtocol.openRpcFunction).args(),
    Duration.Inf
  ))

  // Retrieve the remote API schema in OpenAPI format
  println(Await.result(
    client.call[OpenApi](JsonRpcProtocol.openApiFunction).args(),
    Duration.Inf
  ))

  // Close the client
  Await.result(client.close(), Duration.Inf)

  // Stop the server
  Await.result(server.close(), Duration.Inf)
}

class ApiSchemaDiscovery extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      ApiSchemaDiscovery.main(Array())
    }
  }
}

package examples.basic

import automorph.Default
import automorph.protocol.JsonRpcProtocol
import automorph.transport.http.HttpMethod
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object ApiDescription extends App {

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

  // Retrieve remote API description in OpenRPC format
  val openRpcFunction = JsonRpcProtocol.openRpcFunction
  val openRpcSpec = client.call[Default.Node](openRpcFunction).args()
    .map(client.protocol.codec.text) // Future[String]
  println(Await.result(openRpcSpec, Duration.Inf))

  // Retrieve remote API description in OpenAPI format
  val openApiFunction = JsonRpcProtocol.openApiFunction
  val openApiSpec = client.call[Default.Node](openApiFunction).args()
    .map(client.protocol.codec.text) // Future[String]
  println(Await.result(openApiSpec, Duration.Inf))

  // Close the client
  Await.result(client.close(), Duration.Inf)

  // Stop the server
  Await.result(server.close(), Duration.Inf)
}

class ApiDescription extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      ApiDescription.main(Array())
    }
  }
}

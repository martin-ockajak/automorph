package examples.basic

import automorph.Default
import automorph.transport.http.HttpMethod
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object AsynchronousCall extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new ServerApi()

  // Start JSON-RPC HTTP server listening on port 7000 for PUT requests to '/api'
  val createServer = Default.serverAsync(7000, "/api", Seq(HttpMethod.Put))
  val server = createServer(_.bind(api))

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): Future[String]
  }
  // Setup JSON-RPC HTTP client sending PUT requests to 'http://localhost:7000/api'
  val client = Default.clientAsync(new URI("http://localhost:7000/api"), HttpMethod.Put)

  // Call the remote API function and print the result
  val remoteApi = client.bind[ClientApi]
  println(Await.result(
    remoteApi.hello("world", 1),
    Duration.Inf
  ))

  // Close the client
  Await.result(client.close(), Duration.Inf)

  // Stop the server
  Await.result(server.close(), Duration.Inf)
}

class AsynchronousCall extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" in {
      AsynchronousCall.main(Array())
    }
  }
}

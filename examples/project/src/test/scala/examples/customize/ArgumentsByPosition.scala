package examples.customize

import automorph.transport.http.HttpMethod
import automorph.{Client, Default}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object ArgumentsByPosition extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new ServerApi()

  // Start JSON-RPC HTTP server listening on port 7000 for PUT requests to '/api'
  val serverBuilder = Default.serverBuilderAsync(7000, "/api", Seq(HttpMethod.Put))
  val server = serverBuilder(_.bind(api))

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): Future[String]
  }

  // Configure JSON-RPC to pass arguments by position instead of by name
  val protocol = Default.protocol[Default.ClientContext].namedArguments(false)

  // Setup custom JSON-RPC HTTP client sending PUT requests to 'http://localhost:7000/api'
  val url = new URI("http://localhost:7000/api")
  val clientTransport = Default.clientTransportAsync(url, HttpMethod.Put)
  val client = Client.protocol(protocol).transport(clientTransport)

  // Call the remote API function
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

class ArgumentsByPosition extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" in {
      ArgumentsByPosition.main(Array())
    }
  }
}

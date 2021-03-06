package examples.select

import automorph.Default
import automorph.transport.http.endpoint.UndertowHttpEndpoint
import io.undertow.{Handlers, Undertow}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object EndpointTransport extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new ServerApi()

  // Create custom Undertow JSON-RPC endpoint
  val handler = Default.handlerAsync[UndertowHttpEndpoint.Context]
  val endpoint = UndertowHttpEndpoint(handler.bind(api))

  // Start Undertow JSON-RPC HTTP server listening on port 7000 for requests to '/api'
  val server = Undertow.builder()
    .addHttpListener(7000, "0.0.0.0")
    .setHandler(Handlers.path().addPrefixPath("/api", endpoint))
    .build()

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): Future[String]
  }
  // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
  val client = Default.clientAsync(new URI("http://localhost:7000/api"))

  // Call the remote API function via proxy
  val remoteApi = client.bind[ClientApi] // ClientApi
  remoteApi.hello("world", 1) // Future[String]

  // Close the client
  Await.result(client.close(), Duration.Inf)

  // Stop the server
  server.stop()
}

class EndpointTransport extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" in {
      EndpointTransport.main(Array())
    }
  }
}

package test.examples.select

import automorph.Default
import automorph.transport.http.endpoint.UndertowHttpEndpoint
import io.undertow.{Handlers, Undertow}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EndpointTransportSelection extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new ServerApi()

  // Create custom Undertow JSON-RPC endpoint
  val handler = Default.handlerAsync[UndertowHttpEndpoint.Context]
  val endpoint = UndertowHttpEndpoint(handler.bind(api))

  // Start Undertow JSON-RPC HTTP server listening on port 8080 for requests to '/api'
  val server = Undertow.builder()
    .addHttpListener(8080, "0.0.0.0")
    .setHandler(Handlers.path().addPrefixPath("/api", endpoint))
    .build()

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): Future[String]
  }
  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.clientAsync(new URI("http://localhost/api"))

  // Call the remote API function via proxy
  val remoteApi = client.bind[ClientApi] // ClientApi
  remoteApi.hello("world", 1) // Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.stop()
}

class EndpointTransportSelection extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      EndpointTransportSelection.main(Array())
    }
  }
}

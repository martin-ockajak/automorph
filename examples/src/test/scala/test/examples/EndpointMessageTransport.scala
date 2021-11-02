package test.examples

import automorph.Default
import automorph.transport.http.endpoint.UndertowHttpEndpoint
import io.undertow.{Handlers, Undertow}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EndpointMessageTransport extends App {

  // Define an API type and create its instance
  class Api {
    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new Api()

  // Create custom Undertow JSON-RPC endpoint
  val handler = Default.handlerAsync[UndertowHttpEndpoint.Context]
  val endpoint = UndertowHttpEndpoint(handler.bind(api))

  // Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val server = Undertow.builder()
    .addHttpListener(80, "0.0.0.0")
    .setHandler(Handlers.path().addPrefixPath("/api", endpoint))
    .build()

  // Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.clientAsync(new URI("http://localhost/api"), "POST")

  // Call the remote API function via proxy
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 1) // Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.stop()
}

class EndpointMessageTransport extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      EndpointMessageTransport.main(Array())
    }
  }
}

package test.examples

import io.undertow.{Handlers, Undertow}
import automorph.{DefaultHandler, DefaultHttpClient}
import automorph.transport.http.endpoint.UndertowHttpEndpoint
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EndpointMessageTransport extends App {

  // Define an API type and create API instance
  class Api {
    def hello(some: String, n: Int): Future[String] = Future(s"Hello $some $n!")
  }
  val api = new Api()

  // Start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val handler = DefaultHandler.async[UndertowHttpEndpoint.Context]
  val endpoint = UndertowHttpEndpoint(handler.bind(api), identity)
  val pathHandler = Handlers.path().addPrefixPath("/api", endpoint)
  val server = Undertow.builder()
    .addHttpListener(80, "0.0.0.0")
    .setHandler(pathHandler)
    .build()

  // Create RPC client for sending HTTP POST requests to 'http://localhost/api'
  val url = new java.net.URI("http://localhost/api")
  val client = DefaultHttpClient.async(url, "POST")

  // Call the remote API method via proxy
  val apiProxy = client.bind[Api] // Api
  apiProxy.hello("world", 1) // : Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.stop()
}

class EndpointMessageTransport extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      EndpointMessageTransport.main(Array())
    }
  }
}

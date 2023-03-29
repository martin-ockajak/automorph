package examples.transport

import automorph.{Default, Endpoint}
import automorph.transport.http.endpoint.UndertowHttpEndpoint
import io.undertow.{Handlers, Undertow}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

private[examples] object EndpointTransport {
  @scala.annotation.nowarn
  def main(arguments: Array[String]): Unit = {

    // Define a helper function to evaluate Futures
    def run[T](effect: Future[T]): T = Await.result(effect, Duration.Inf)

    // Create server API instance
    class ServerApi {
      def hello(some: String, n: Int): Future[String] =
        Future(s"Hello $some $n!")
    }
    val api = new ServerApi()

    // Create Undertow JSON-RPC endpoint transport
    val endpointTransport = UndertowHttpEndpoint(Default.effectSystemAsync)

    // Setup JSON-RPC endpoint
    val endpoint = Endpoint.transport(endpointTransport).rpcProtocol(Default.rpcProtocol).bind(api)

    // Start Undertow HTTP server listening on port 7000 for requests to '/api'
    val server = Undertow.builder()
      .addHttpListener(7000, "0.0.0.0")
      .setHandler(Handlers.path().addPrefixPath("/api", endpoint.adapter))
      .build()
    server.start()

    // Define client view of the remote API
    trait ClientApi {
      def hello(some: String, n: Int): Future[String]
    }

    // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
    val client = run(
      Default.clientAsync(new URI("http://localhost:7000/api")).init()
    )

    // Call the remote API function via proxy
    val remoteApi = client.bind[ClientApi]
    println(run(
      remoteApi.hello("world", 1)
    ))

    // Close the client
    run(client.close())

    // Stop the server
    server.stop()
  }
}

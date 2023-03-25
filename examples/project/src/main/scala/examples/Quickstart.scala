package examples

import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

private[examples] object Quickstart {
  @scala.annotation.nowarn
  def main(arguments: Array[String]): Unit = {

    // Create server API instance
    class ServerApi {

      def hello(some: String, n: Int): Future[String] =
        Future(s"Hello $some $n!")
    }
    val api = new ServerApi()

    // Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
    val serverBuilder = Default.serverBuilderAsync(7000, "/api")
    val server = serverBuilder(_.bind[ServerApi](api))

    // Define client view of the remote API
    trait ClientApi {
      def hello(some: String, n: Int): Future[String]
    }

    // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
    val client = Default.clientAsync(new URI("http://localhost:7000/api"))

    // Call the remote API function statically
    val remoteApi = client.bind[ClientApi]
    println(Await.result(
      remoteApi.hello("world", 1),
      Duration.Inf
    ))

    // Call the remote API function dynamically
    val x = implicitly[Default.ClientContext]
    println(x)
    println(Await.result(
      client.call[String]("hello").args("some" -> "world", "n" -> 1),
      Duration.Inf
    ))

    // Close the client
    Await.result(client.close(), Duration.Inf)

    // Stop the server
    Await.result(server.close(), Duration.Inf)
  }
}

package examples.basic

import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object OneWayMessage {
  def main(arguments: Array[String]): Unit = {

    // Create server API instance
    class ServerApi {
      def hello(some: String, n: Int): Future[String] =
        Future(s"Hello $some $n!")
    }
    val api = new ServerApi()

    // Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
    val serverBuilder = Default.serverBuilderAsync(7000, "/api")
    val server = serverBuilder(_.bind(api))

    // Define client view of the remote API
    trait ClientApi {
      def hello(some: String, n: Int): Future[String]
    }
    // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
    val client = Default.clientAsync(new URI("http://localhost:7000/api"))

    // Message the remote API function dynamically without expecting a response
    Await.result(
      client.message("hello").args("some" -> "world", "n" -> 1),
      Duration.Inf
    )

    // Close the client
    Await.result(client.close(), Duration.Inf)

    // Stop the server
    Await.result(server.close(), Duration.Inf)
  }
}

class OneWayMessage extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" in {
      OneWayMessage.main(Array())
    }
  }
}

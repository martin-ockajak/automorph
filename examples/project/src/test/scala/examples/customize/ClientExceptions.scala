package examples.customize

import automorph.{Client, Default}
import java.net.URI
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try

object ClientExceptions extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): Future[String] =
      Future.failed(new IllegalArgumentException("Test error"))
  }
  val api = new ServerApi()

  // Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
  val serverBuilder = Default.serverBuilderAsync(7000, "/api")
  val server = serverBuilder(_.bind(api))

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): Future[String]
  }

  // Customize remote API client RPC error to exception mapping
  val protocol = Default.protocol[Default.ClientContext].mapError((message, code) =>
    if (message.contains("Data")) {
      new SQLException(message)
    } else {
      Default.protocol.mapError(message, code)
    }
  )

  // Setup custom JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
  val transport = Default.clientTransportAsync(new URI("http://localhost:7000/api"))
  val client = Client.protocol(protocol).transport(transport)

  // Call the remote API function and fail with SQLException
  val remoteApi = client.bind[ClientApi]
  println(Try(Await.result(
    remoteApi.hello("world", 1),
    Duration.Inf
  )).failed.get)

  // Close the client
  Await.result(client.close(), Duration.Inf)

  // Stop the server
  Await.result(server.close(), Duration.Inf)
}

class ClientExceptions extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" in {
      ClientExceptions.main(Array())
    }
  }
}

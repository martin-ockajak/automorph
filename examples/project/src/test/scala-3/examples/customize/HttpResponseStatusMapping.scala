package examples.customize

import automorph.Default
import automorph.transport.http.HttpContext
import java.net.URI
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object HttpResponseStatusMapping extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new ServerApi()

  // Customize remote API server exception to HTTP status code mapping
  val createServer = Default.serverAsync(7000, "/api", mapException = {
    case _: SQLException => 400
    case e => HttpContext.defaultExceptionToStatusCode(e)
  })

  // Start custom JSON-RPC HTTP server listening on port 7000 for requests to '/api'
  val server = createServer(_.bind(api))

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): Future[String]
  }
  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
  val client = Default.clientAsync(new URI("http://localhost:7000/api"))

  // Call the remote API function
  val remoteApi = client.bind[ClientApi] // ClientApi
  remoteApi.hello("world", 1) // Future[String]

  // Close the client
  Await.result(client.close(), Duration.Inf)

  // Stop the server
  Await.result(server.close(), Duration.Inf)
}

class HttpResponseStatusMapping extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" in {
      HttpResponseStatusMapping.main(Array())
    }
  }
}

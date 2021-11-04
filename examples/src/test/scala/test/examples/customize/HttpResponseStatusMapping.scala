package test.examples.customize

import automorph.Default
import automorph.transport.http.HttpContext
import java.net.URI
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object HttpResponseStatusMapping extends App {

  // Define an API type and create its instance
  class Api {
    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new Api()

  // Customize remote API server exception to HTTP status code mapping
  val createServer = Default.serverAsync(80, "/api", mapException = {
    case _: SQLException => 400
    case e => HttpContext.defaultExceptionToStatusCode(e)
  })

  // Start custom JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val server = createServer(_.bind(api))

  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.clientAsync(new URI("http://localhost/api"))

  // Call the remote API function
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 1) // Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class HttpResponseStatusMapping extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      HttpResponseStatusMapping.main(Array())
    }
  }
}

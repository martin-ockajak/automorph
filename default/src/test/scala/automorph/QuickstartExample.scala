package automorph

import io.undertow.server.HttpServerExchange
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object QuickstartExample extends App {
  class Api {
    def hello(thing: String): Future[String] = Future.successful(s"Hello $thing!")
  }

  val api = new Api()

  // Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = DefaultHttpServer.async(_.bind(api), 80, "/api")

  // Create JSON-RPC client sending HTTP POST requests to 'http://localhost/api'
  val client = DefaultHttpClient.async("http://localhost/api", "POST")

  // Proxy call
  val apiProxy = client.bind[Api]
  apiProxy.hello("world") // : Future[String]

  // Direct call passing arguments by name
  client.method("hello").namedArgs("thing" -> "world").call[String] // : Future[String]

  // Direct notification passing arguments by position
  client.method("hello").args("world").tell // : Future[Unit]

  // Stop the server
  server.close()
}

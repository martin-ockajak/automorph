package automorph

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class QuickstartExample {
  class Api {
    def hello(thing: String): Future[String] = Future.successful(s"Hello $thing!")
  }

  val api = new Api()

  // Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = DefaultHttpServer.async(_.bind(api), 80, "/api")

  // Create JSON-RPC client sending HTTP POST requests to 'http://localhost/api'
  val client = DefaultHttpClient.async("http://localhost/api", "POST")

  // Proxy call
  val apiProxy = client.bindByName[Api]
  val proxyResult = apiProxy.hello("world") // : Future[String]

  // Direct call
  val directResult: Future[String] = client.callByName[String, String]("hello", "thing" -> "world")

  // Direct notification
  client.notifyByName("hello", "thing" -> "world") // : Future[Unit]

  // Stop the server
  server.close()
}

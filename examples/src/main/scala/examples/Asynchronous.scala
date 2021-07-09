package examples

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Asynchronous extends App {

  // Define an API type and create API instance
  class Api {
    def hello(some: String, n: Int): Future[String] = Future.successful(s"Hello $some $n!")
  }
  val api = new Api()

  // Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = automorph.DefaultHttpServer.async(_.bind(api), 80, "/api")

  // Create JSON-RPC client for sending HTTP POST requests to 'http://localhost/api'
  val client = automorph.DefaultHttpClient.async("http://localhost/api", "POST")

  // Call the remote API method via proxy
  val apiProxy = client.bind[Api] // Api
  apiProxy.hello("world", 1) // : Future[String]

  // Call a remote API method dynamically passing the arguments by name
  val hello = client.method("hello")
  hello.args("some" -> "world", "n" -> 1).call[String] // Future[String]

  // Call a remote API method dynamically passing the arguments by position
  hello.positional.args("world", 1).call[String] // Future[String]

  // Notify a remote API method dynamically passing the arguments by name
  hello.args("some" -> "world", "n" -> 1).tell // Future[Unit]

  // Notify a remote API method dynamically passing the arguments by position
  hello.positional.args("world", 1).tell // Future[Unit]

  // Stop the server
  server.close()
}

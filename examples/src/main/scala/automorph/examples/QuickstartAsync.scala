package automorph.example

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object QuickstartAsync extends App {

  class AsyncApi {
    def hello(some: String, n: Int): Future[String] = Future.successful(s"Hello $some $n!")
  }

  val asyncApi = new AsyncApi() // AsyncApi

  // Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
  val asyncServer = automorph.DefaultHttpServer.async(_.bind(asyncApi), 80, "/api")

  // Create JSON-RPC client sending HTTP POST requests to 'http://localhost/api'
  val asyncClient = automorph.DefaultHttpClient.async("http://localhost/api", "POST")

  // Call the remote API method via proxy
  val asyncApiProxy = asyncClient.bind[AsyncApi] // AsyncApi
  asyncApiProxy.hello("world", 1) // : Future[String]

  // Call a remote API method passing the arguments by name
  val hello = asyncClient.method("hello")
  hello.args("some" -> "world", "n" -> 1).call[String] // Future[String]

  // Call a remote API method passing the arguments by position
  hello.positional.args("world", 1).call[String] // Future[String]

  // Notify a remote API method passing the arguments by name
  hello.args("some" -> "world", "n" -> 1).tell // Future[Unit]

  // Notify a remote API method passing the arguments by position
  hello.positional.args("world", 1).tell // Future[Unit]

  // Stop the server
  asyncServer.close()
}

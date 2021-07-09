package examples

import zio.{Runtime, Task}
import org.asynchttpclient.DefaultAsyncHttpClient
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend

object CustomBackend extends App {

  // Define an API type and create API instance
  class Api {
    def hello(some: String, n: Int): Task[String] = Task.succeed(s"Hello $some $n!")
  }
  val api = new Api()

  // Custom effectful computation backend plugin
  val backend = automorph.backend.ZioBackend[Any]()
  val runEffect = (effect: Task[_]) => Runtime.default.unsafeRunTask(effect)

  // Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = automorph.DefaultHttpServer(backend, runEffect, _.bind(api), 80, "/api")

  // Create JSON-RPC client for sending HTTP POST requests to 'http://localhost/api'
  val sttpBackend = AsyncHttpClientZioBackend.usingClient(Runtime.default, new DefaultAsyncHttpClient())
  val client = automorph.DefaultHttpClient("http://localhost/api", "POST", backend, sttpBackend)

  // Call the remote API method via proxy
  val apiProxy = client.bind[Api] // Api
  apiProxy.hello("world", 1) // : Task[String]

  // Stop the server
  server.close()
}

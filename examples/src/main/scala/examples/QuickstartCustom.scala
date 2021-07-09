package examples

import zio.{Runtime, Task}
import org.asynchttpclient.DefaultAsyncHttpClient
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend

object QuickstartCustom extends App {

  class CustomApi {
    def hello(some: String, n: Int): Task[String] = Task.succeed(s"Hello $some $n!")
  }

  val customApi = new CustomApi()

  // Custom effectful computation backend plugin
  val backend = automorph.backend.ZioBackend[Any]()
  val runEffect = (effect: Task[_]) => Runtime.default.unsafeRunTask(effect)

  // Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
//  val customServer = automorph.DefaultHttpServer(backend, runEffect, (handler: automorph.DefaultTypes.DefaultHandler[Task, HttpServerExchange]) => handler.bind(customApi), 80, "/api")
  val customServer = automorph.DefaultHttpServer(backend, runEffect, _.bind(customApi), 80, "/api")

  // Create JSON-RPC client sending HTTP POST requests to 'http://localhost/api'
  val sttpBackend = AsyncHttpClientZioBackend.usingClient(Runtime.default, new DefaultAsyncHttpClient())
  val customClient = automorph.DefaultHttpClient("http://localhost/api", "POST", backend, sttpBackend)

  // Call the remote API method via proxy
  val customApiProxy = customClient.bind[CustomApi] // CustomApi
  customApiProxy.hello("world", 1) // : Task[String]

  // Stop the server
  customServer.close()
}

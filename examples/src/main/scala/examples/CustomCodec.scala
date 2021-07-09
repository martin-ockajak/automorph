package examples

import automorph.{Client, DefaultBackend, DefaultCodec, DefaultHttpTransport}
import org.asynchttpclient.DefaultAsyncHttpClient
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object QuickstartAsyncCodec extends App {

  class AsyncApi {
    def hello(some: String, n: Int): Future[String] = Future.successful(s"Hello $some $n!")
  }

  val asyncApi = new AsyncApi() // AsyncApi

  // Async effectful computation backend plugin
  val backend = automorph.backend.FutureBackend()
  val runEffect = (effect: Future[_]) => effect

  // Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
  val asyncServer = automorph.DefaultHttpServer(backend, runEffect, _.bind(asyncApi), 80, "/api")

  // Create JSON-RPC client sending HTTP POST requests to 'http://localhost/api'
  val transport = DefaultHttpTransport.async("http://localhost/api", "POST")
  val asyncClient = Client(DefaultCodec(), DefaultBackend.async, transport)

  // Call the remote API method via proxy
  val asyncApiProxy = asyncClient.bind[AsyncApi] // AsyncApi
  asyncApiProxy.hello("world", 1) // : Future[String]

  // Stop the server
  asyncServer.close()
}

package test.examples.basic

import automorph.Default
import automorph.transport.http.HttpMethod
import java.net.URI

object SynchronousCall extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): String =
      s"Hello $some $n!"
  }
  val api = new ServerApi()

  // Start default JSON-RPC HTTP server listening on port 8080 for POST requests to '/api'
  val createServer = Default.serverSync(8080, "/api", Seq(HttpMethod.Post))
  val server = createServer(_.bind(api))

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): String
  }
  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.clientSync(new URI("http://localhost/api"), HttpMethod.Post)

  // Call the remote API function
  val remoteApi = client.bind[ClientApi] // ClientApi
  remoteApi.hello("world", 1) // String

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class SynchronousCall extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      SynchronousCall.main(Array())
    }
  }
}

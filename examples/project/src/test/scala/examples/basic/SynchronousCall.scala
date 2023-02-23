package examples.basic

import automorph.Default
import automorph.transport.http.HttpMethod
import java.net.URI

object SynchronousCall {
  def main(arguments: Array[String]): Unit = {

    // Create server API instance
    class ServerApi {
      def hello(some: String, n: Int): String =
        s"Hello $some $n!"
    }
    val api = new ServerApi()

    // Start JSON-RPC HTTP server listening on port 7000 for POST requests to '/api'
    val serverBuilder = Default.serverBuilderSync(7000, "/api", Seq(HttpMethod.Post))
    val server = serverBuilder(_.bind(api))

    // Define client view of the remote API
    trait ClientApi {
      def hello(some: String, n: Int): String
    }
    // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
    val client = Default.clientSync(new URI("http://localhost:7000/api"), HttpMethod.Post)

    // Call the remote API function
    val remoteApi = client.bind[ClientApi]
    remoteApi.hello("world", 1)

    // Close the client
    client.close()

    // Stop the server
    server.close()
  }
}

class SynchronousCall extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Example" in {
      SynchronousCall.main(Array())
    }
  }
}

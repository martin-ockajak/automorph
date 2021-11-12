package examples.customize

import automorph.Default
import java.net.URI

object ClientFunctionNames extends App {

  // Create server API instance
  class ServerApi {
    // Exposed both as 'hello' and 'hi'
    def hello(some: String, n: Int): String =
      s"Hello $some $n!"
  }
  val api = new ServerApi()

  // Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
  val createServer = Default.serverSync(7000, "/api")
  val server = createServer(_.bind(api))

  // Define client view of a remote API
  trait ClientApi {
    // Invoked as 'hello'
    def hi(some: String, n: Int): String
  }

  // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
  val client = Default.clientSync(new URI("http://localhost:7000/api"))

  // Customize invoked API to RPC function name mapping
  val mapName = (name: String) => name match {
    case "hi" => "hello"
    case other => other
  }

  // Call the remote API function
  val remoteApi = client.bind[ClientApi](mapName) // ClientApi
  remoteApi.hi("world", 1) // String

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class ClientFunctionNames extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      ClientFunctionNames.main(Array())
    }
  }
}


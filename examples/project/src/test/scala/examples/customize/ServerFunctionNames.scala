package examples.customize

import automorph.Default
import java.net.URI
import scala.util.Try

object ServerFunctionNames extends App {

  // Create server API instance
  class ServerApi {
    // Exposed both as 'hello' and 'hi'
    def hello(some: String, n: Int): String =
      s"Hello $some $n!"

    // Not exposed
    def skip(): String =
      ""

    // Exposed as 'test.welcome'
    def welcome(who: Boolean): String =
      s"Welcome $who"
  }
  val api = new ServerApi()

  // Customize exposed API to RPC function name mapping
  val mapName = (name: String) => name match {
    case "hello" => Seq("hello", "hi")
    case "skip" => Seq.empty
    case other => Seq(s"test.$other")
  }

  // Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
  val createServer = Default.serverSync(7000, "/api")
  val server = createServer(_.bind(api, mapName))

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): String

    def hi(some: String, n: Int): String
  }

  // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
  val client = Default.clientSync(new URI("http://localhost:7000/api"))

  // Call the remote API function statically
  val remoteApi = client.bind[ClientApi] // ClientApi
  remoteApi.hello("world", 1) // String
  remoteApi.hi("world", 1) // String

  // Call the remote API function dynamically
  Try(client.call[String]("skip").args()) // Failure - method not found
  client.call[Double]("test.welcome").args("who" -> "all") // String

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class ServerFunctionNames extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      ServerFunctionNames.main(Array())
    }
  }
}


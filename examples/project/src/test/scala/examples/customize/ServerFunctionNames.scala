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

    // Exposed as 'test.welcome'
    def welcome(who: Boolean): String =
      s"Welcome $who"

    // Not exposed
    def hidden(): String =
      ""
  }
  val api = new ServerApi()

  // Customize exposed API to RPC function name mapping
  val mapName = (name: String) => name match {
    case "hello" => Seq("hello", "hi")
    case "hidden" => Seq.empty
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
  val remoteApi = client.bind[ClientApi]
  println(
    remoteApi.hello("world", 1)
  )
  println(
    remoteApi.hi("world", 1)
  )

  // Call the remote API function dynamically
  print(
    client.call[Double]("test.welcome").args("who" -> "all")
  )

  // Call the remote API function dynamically and fail with FunctionNotFoundException
  println(Try(
    client.call[String]("hidden").args()
  ).failed.get)

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


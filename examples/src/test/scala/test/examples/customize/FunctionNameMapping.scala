package test.examples.customize

import automorph.Default
import java.net.URI
import scala.util.Try

object FunctionNameMapping extends App {

  // Create server API instance
  class ServerApi {
    // Exposed both as 'hello' and 'custom'
    def hello(value: Option[String]): String =
      value.getOrElse("")

    // Not exposed
    def omitted(): String =
      ""

    // Exposed as 'test.multi'
    def multi(add: Boolean)(n: Double): Double =
      if (add) n + 1 else n - 1
  }
  val api = new ServerApi()

  // Customize RPC function names
  val mapName = (name: String) => name match {
    case "hello" => Seq("hello", "custom")
    case "omitted" => Seq.empty
    case other => Seq(s"test.$other")
  }

  // Start default JSON-RPC HTTP server listening on port 8080 for requests to '/api'
  val createServer = Default.serverSync(8080, "/api")
  val server = createServer(_.bind(api, mapName(_)))

  // Define client view of a remote API
  trait ClientApi {
    def hello(value: Option[String]): String

    def custom(value: Option[String]): String
  }

  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.clientSync(new URI("http://localhost/api"))

  // Call the remote API function statically
  val remoteApi = client.bind[ClientApi] // ClientApi
  remoteApi.hello(None) // ""
  remoteApi.custom(None) // ""

  // Call the remote API function dynamically
  Try(client.call[String]("omitted").args()) // Failure
  client.call[Double]("test.multi").args("add" -> true, "n" -> 1) // 2

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class FunctionNameMapping extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      FunctionNameMapping.main(Array())
    }
  }
}


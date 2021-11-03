package test.examples

import automorph.Default
import java.net.URI
import scala.util.Try

object FunctionNameMapping extends App {

  // Define an API type and create its instance
  class Api {
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
  val api = new Api()

  // Customize RPC function names
  val mapName = (name: String) => name match {
    case "hello" => Seq("hello", "custom")
    case "omitted" => Seq.empty
    case other => Seq(s"test.$other")
  }

  // Start default JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val createServer = Default.serverSync(80, "/api")
  val server = createServer(_.bind(api, mapName(_)))

  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.clientSync(new URI("http://localhost/api"))

  // Call the remote API function dynamically
  client.call[String]("custom").args("value" -> None) // ""
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


package test.examples

import automorph.Default
import java.net.URI
import scala.util.Try

object NameMapping extends App {

  // Define an API type and create its instance
  class Api {
    // Exposed both as 'original' and 'custom'
    def original(value: Option[String]): String =
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
  val mapNames = (name: String) => name match {
    case "original" => Seq("original", "custom")
    case "omitted" => Seq.empty
    case other => Seq(s"test.$other")
  }

  // Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val server = Default.serverSync(_.bind(api, mapNames(_)), 80, "/api")

  // Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.clientSync(new URI("http://localhost/api"), "POST")

  // Call the remote API function dynamically
  client.function("test.multiParams").args("add" -> true, "n" -> 1).call[Double] // 2
  client.function("aliased").args("value" -> None).tell // ()
  Try(client.function("omitted").args().call[String]) // Failure

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class NameMapping extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      NameMapping.main(Array())
    }
  }
}


package examples

import scala.util.Try

object MethodAlias extends App {

  // Define an API type and create API instance
  class Api {
    // Exposed as 'test.regular'
    def regular(add: Boolean, n: Double): Double = if (add) n + 1 else n - 1

    // Exposed as 'original' and 'aliased'
    def original(value: Option[String]): String = value.getOrElse("")

    // Not exposed
    def omitted(): String = ""
  }
  val api = new Api()

  // Customize method names
  val mapMethodName = (name: String) => name match {
    case "original" => Seq("original", "aliased")
    case "omitted" => Seq()
    case other => Seq(s"test.$other")
  }

  // Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = automorph.DefaultHttpServer.sync(_.bind(api, mapMethodName(_)), 80, "/api")

  // Create JSON-RPC client for sending HTTP POST requests to 'http://localhost/api'
  val client = automorph.DefaultHttpClient.sync("http://localhost/api", "POST")

  // Call the remote API method via proxy
  client.method("test.regular").args("add" -> true, "n" -> 1).call[Double] // 2
  client.method("aliased").args("value" -> None).tell // ()
  Try(client.method("omitted").args().call[String]) // Failure

  // Stop the server
  server.close()
}

package test.examples

import automorph.transport.http.client.SttpClient.defaultContext

object MethodMapping extends App {

  // Define an API type and create API instance
  class Api {
    // Exposed as 'test.multiParams'
    def multiParams(add: Boolean)(n: Double): Double = if (add) n + 1 else n - 1

    // Exposed as 'original' and 'aliased'
    def original(value: Option[String]): String = value.getOrElse("")

    // Not exposed
    def omitted(): String = ""
  }
  val api = new Api()

  // Customize method name mapping
  val methodAliases = (name: String) => name match {
    case "original" => Seq("original", "aliased")
    case "omitted" => Seq()
    case other => Seq(s"test.$other")
  }

  // Start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = automorph.DefaultHttpServer.sync(_.bind(api, methodAliases(_)), 80, "/api")

  // Create RPC client for sending HTTP POST requests to 'http://localhost/api'
  val url = new java.net.URI("http://localhost/api")
  val client = automorph.DefaultHttpClient.sync(url, "POST")

  // Call the remote API method via proxy
  client.method("test.multiParams").args("add" -> true, "n" -> 1).call[Double] // 2
  client.method("aliased").args("value" -> None).tell // ()
  util.Try(client.method("omitted").args().call[String]) // Failure

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class MethodMapping extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      MethodMapping.main(Array())
    }
  }
}

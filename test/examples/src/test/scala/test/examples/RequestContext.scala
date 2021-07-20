package test.examples

object RequestContext extends App {

  // Define server API type and create API instance
  class ServerApi {
    import automorph.DefaultHttpServer.Context

    // Use request context provided by the server transport
    def requestMetaData(message: String)(implicit context: Context): List[String] =
      List(Some(message), context.path, context.header("X-Test")).flatten
  }
  val api = new ServerApi()

  // Define client view of the server API
  trait ClientApi {
    import automorph.DefaultHttpClient.Context

    // Supply request context used by the client transport
    def requestMetaData(message: String)(implicit context: Context): List[String]
  }

  // Create and start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = automorph.DefaultHttpServer.sync(_.bind(api), 80, "/api")

  // Create RPC client for sending HTTP POST requests to 'http://localhost/api'
  val url = new java.net.URI("http://localhost/api")
  val client = automorph.DefaultHttpClient.sync(url, "POST")

  // Create context for requests sent by the client
  val apiProxy = client.bind[ClientApi] // Api
  val defaultContext = client.context
  implicit val context: automorph.DefaultHttpClient.Context = defaultContext.header("X-Test", "valid")

  // Call the remote API method via proxy
  apiProxy.requestMetaData("test") // List("test", "/api", "valid")
  apiProxy.requestMetaData("test")(context) // List("test", "/api", "valid")
  client.method("requestMetaData").args("message" -> "test").call[List[String]] //  List("test", "/api", "valid")

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class RequestContext extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      RequestContext.main(Array())
    }
  }
}

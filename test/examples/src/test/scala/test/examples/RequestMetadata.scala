package test.examples

object RequestMetadata extends App {

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

  // Start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = automorph.DefaultHttpServer.sync(_.bind(api), 80, "/api")

  // Create RPC client for sending HTTP POST requests to 'http://localhost/api'
  val url = new java.net.URI("http://localhost/api")
  val client = automorph.DefaultHttpClient.sync(url, "POST")

  // Create client request context specifying HTTP request meta-data
  val apiProxy = client.bind[ClientApi] // Api
  val context = client.context
    .queryParameters("test" -> "value")
    .headers("X-Test" -> "value")
    .cookies("Test" -> "value")
    .authorizationBearer("value")

  // Call the remote API method via proxy supplying the request context directly
  apiProxy.requestMetaData("test")(context) // List("test", "/api", "valid")
  client.method("requestMetaData").args("message" -> "test").call[List[String]] //  List("test", "/api", "valid")

  // Call the remote API method via proxy supplying the request context as an implicit argument
  implicit lazy val implicitContext: automorph.DefaultHttpClient.Context = context
  apiProxy.requestMetaData("test") // List("test", "/api", "valid")
  client.method("requestMetaData").args("message" -> "test").call[List[String]] //  List("test", "/api", "valid")

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class RequestMetadata extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      RequestMetadata.main(Array())
    }
  }
}

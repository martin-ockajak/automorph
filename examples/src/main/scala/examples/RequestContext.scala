package examples

object RequestContext extends App {

  // Define server API type and create API instance
  class ServerApi {
    import automorph.DefaultHttpServer.Context

    // Use request context provided by the server transport
    def requestMetaData(message: String)(implicit context: Context): List[String] =
      List(message, context.getRequestPath, context.getRequestHeaders.get("X-Test").peek)
  }
  val api = new ServerApi()

  // Define client view of the server API
  trait ClientApi {
    import automorph.DefaultHttpClient.Context

    // Supply request context used by the client transport
    def requestMetaData(message: String)(implicit context: Context): List[String]
  }

  // Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = automorph.DefaultHttpServer.sync(_.bind(api), 80, "/api")

  // Create JSON-RPC client for sending HTTP POST requests to 'http://localhost/api'
  val client = automorph.DefaultHttpClient.sync("http://localhost/api", "POST")

  // Create context for requests sent by the client
  val apiProxy = client.bind[ClientApi] // Api
  val defaultContext = client.defaultContext
  implicit val context: automorph.DefaultHttpClient.Context = defaultContext.copy(
    partial = defaultContext.partial.header("X-Test", "valid")
  )

  // Call the remote API method via proxy
  apiProxy.requestMetaData("test") // List("test", "/api", "valid")
  apiProxy.requestMetaData("test")(context) // List("test", "/api", "valid")
  client.method("requestMetaData").args("message" -> "test").call[List[String]] //  List("test", "/api", "valid")

  // Stop the server
  server.close()
}

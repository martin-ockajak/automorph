package test.examples

object RequestMetadata extends App {

  // Define server API type and create API instance
  class ServerApi {

    import automorph.DefaultHttpServer.Context

    // Use HTTP request metadata context provided by the server or endpoint message transport
    def useMetadata(message: String)(implicit context: Context): String = Seq(
      Some(message),
      context.path,
      context.header("X-Test")
    ).flatten.mkString(",")
  }
  val api = new ServerApi()

  // Define client view of the server API
  trait ClientApi {

    import automorph.DefaultHttpClient.Context

    // Supply request context used by the client transport
    def useMetadata(message: String)(implicit context: Context): String
  }

  // Start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = automorph.DefaultHttpServer.sync(_.bind(api), 80, "/api")

  // Create RPC client for sending HTTP POST requests to 'http://localhost/api'
  val url = new java.net.URI("http://localhost/api")
  val client = automorph.DefaultHttpClient.sync(url, "POST")

  // Create client request context specifying HTTP request meta-data
  val apiProxy = client.bind[ClientApi] // Api

  // Create HTTP request metadata context
  val context = client.context
    .queryParameters("test" -> "value")
    .headers("X-Test" -> "value", "Cache-Control" -> "no-cache")
    .cookies("Test" -> "value")
    .authorizationBearer("value")

  // Call the remote API method via proxy with request context supplied directly
  apiProxy.useMetadata("test")(context) // String

  // Call the remote API method dynamically with request context supplied directly
  client.method("useMetadata").args("message" -> "test").call[String] // String

  // Call the remote API method via proxy with request context supplied implictly
  implicit lazy val implicitContext: automorph.DefaultHttpClient.Context = context
  apiProxy.useMetadata("test") // String

  // Call the remote API method dynamically with request context supplied implictly
  client.method("useMetadata").args("message" -> "test").call[String] // String

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

package test.examples

import automorph.Default
import java.net.URI

object RequestMetadata extends App {

  // Define server API type and create its instance
  class ServerApi {

    // Use HTTP request metadata context provided by the server message transport plugin
    def useMetadata(message: String)(implicit requestContext: Default.ServerContext): String = Seq(
      Some(message),
      requestContext.path,
      requestContext.header("X-Test")
    ).flatten.mkString(",")
  }
  val api = new ServerApi()

  // Define client view of the server API
  trait ClientApi {

    // Use HTTP request metadata context defined by the client message transport plugin
    def useMetadata(message: String)(implicit request: Default.ClientContext): String
  }

  // Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val server = Default.syncServer(_.bind(api), 80, "/api")

  // Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.syncClient(new URI("http://localhost/api"), "POST")

  // Create client request context specifying HTTP request meta-data
  val requestMetadata = client.defaultContext
    .parameters("test" -> "value")
    .headers("X-Test" -> "value", "Cache-Control" -> "no-cache")
    .cookies("Test" -> "value")
    .authorizationBearer("value")

  // Call the remote API function statically with request context supplied directly
  val remoteApi = client.bind[ClientApi] // Api
  remoteApi.useMetadata("test")(requestMetadata) // String

  // Call the remote API function statically with request context supplied implictly
  implicit val givenRequestMetadata: Default.ClientContext = requestMetadata
  remoteApi.useMetadata("test") // String

  // Call the remote API function dynamically with request context supplied directly
  val remoteUseMetadata = client.function("useMetadata")
  remoteUseMetadata.args("message" -> "test").call[String] // String

  // Call the remote API function dynamically with request context supplied implictly
  remoteUseMetadata.args("message" -> "test").call[String] // String

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

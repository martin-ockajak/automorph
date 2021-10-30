package test.examples

import automorph.Default
import java.net.URI

object RequestMetadata extends App {

  // Define server API type and create its instance
  class ServerApi {

    // Use HTTP request metadata context provided by the server message transport plugin
    def contextual(message: String)(
      implicit requestContext: Default.ServerContext
    ): String = Seq(
      Some(message),
      requestContext.path,
      requestContext.header("X-Test")
    ).flatten.mkString(",")
  }
  val api = new ServerApi()

  // Define client view of the server API
  trait ClientApi {

    // Use HTTP request context defined by the client message transport plugin
    def contextual(message: String)(implicit request: Default.ClientContext): String
  }

  // Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val createServer = Default.serverSync(80, "/api")
  val server = createServer(_.bind(api))

  // Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.clientSync(new URI("http://localhost/api"), "POST")

  // Create client request context specifying HTTP request meta-data
  val requestContext = client.defaultContext
    .parameters("test" -> "value")
    .headers("X-Test" -> "value", "Cache-Control" -> "no-cache")
    .cookies("Test" -> "value")
    .authorizationBearer("value")

  // Call the remote API function statically with request context supplied directly
  val remoteApi = client.bind[ClientApi] // Api
  remoteApi.contextual("test")(using requestContext) // String

  // Call the remote API function statically with request context supplied implictly
  implicit val givenRequestMetadata: Default.ClientContext = requestContext
  remoteApi.contextual("test") // String

  // Call the remote API function dynamically with request context supplied directly
  val callContextual = client.call[String]("contextual")
  callContextual.args("message" -> "test")(using requestContext) // String

  // Call the remote API function dynamically with request context supplied implictly
  callContextual.args("message" -> "test") // String

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class RequestMetadata extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      RequestMetadata.main(Array())
    }
  }
}
package test.examples

import automorph.Default
import automorph.Default.{ClientContext, ServerContext}
import java.net.URI

object HttpRequestMetadata extends App {

  // Define server API type and create its instance
  class ServerApi {

    // Use HTTP request metadata context provided by the server message transport plugin
    def hello(message: String)(implicit requestContext: ServerContext): String = Seq(
      Some(message),
      requestContext.path,
      requestContext.header("X-Test")
    ).flatten.mkString(",")
  }
  val api = new ServerApi()

  // Define client view of the server API
  trait ClientApi {

    // Use HTTP request context defined by the client message transport plugin
    def hello(message: String)(implicit request: ClientContext): String
  }

  // Start default JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val createServer = Default.serverSync(80, "/api")
  val server = createServer(_.bind(api))

  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.clientSync(new URI("http://localhost/api"))

  // Create client request context specifying HTTP request meta-data
  val requestContext = client.defaultContext
    .parameters("test" -> "value")
    .headers("X-Test" -> "value", "Cache-Control" -> "no-cache")
    .cookies("Test" -> "value")
    .authorizationBearer("value")

  // Call the remote API function statically with request context supplied directly
  val remoteApi = client.bind[ClientApi] // Api
  remoteApi.hello("test")(requestContext) // String

  // Call the remote API function statically with request context supplied implictly
  implicit val givenRequestMetadata: ClientContext = requestContext
  remoteApi.hello("test") // String

  // Call the remote API function dynamically with request context supplied directly
  client.call[String]("hello").args("message" -> "test")(requestContext) // String

  // Call the remote API function dynamically with request context supplied implictly
  client.call[String]("hello").args("message" -> "test") // String

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class HttpRequestMetadata extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      HttpRequestMetadata.main(Array())
    }
  }
}

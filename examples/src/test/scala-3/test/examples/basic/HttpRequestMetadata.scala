package test.examples.basic

import automorph.Default
import automorph.Default.{ClientContext, ServerContext}
import java.net.URI

object HttpRequestMetadata extends App {

  // Create server API instance
  class ServerApi {

    // Accept HTTP request context provided by the server message transport plugin
    def hello(message: String)(implicit http: ServerContext): String = Seq(
      Some(message),
      http.path,
      http.header("X-Test")
    ).flatten.mkString(", ")
  }
  val api = new ServerApi()

  // Start default JSON-RPC HTTP server listening on port 7000 for requests to '/api'
  val createServer = Default.serverSync(7000, "/api")
  val server = createServer(_.bind(api))

  // Define client view of a remote API
  trait ClientApi {

    // Accept HTTP request context consumed by the client message transport plugin
    def hello(message: String)(implicit http: ClientContext): String
  }

  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
  val client = Default.clientSync(new URI("http://localhost:7000/api"))

  // Create client request context specifying HTTP request meta-data
  implicit val http: ClientContext = client.defaultContext
    .parameters("test" -> "value")
    .headers("X-Test" -> "value", "Cache-Control" -> "no-cache")
    .cookies("Test" -> "value")
    .authorizationBearer("value")

  // Call the remote API function statically with implicitly given request context
  val remoteApi = client.bind[ClientApi] // Api
  remoteApi.hello("test") // String

  // Call the remote API function dynamically with implicitly given request context
  client.call[String]("hello").args("message" -> "test") // String

  // Call the remote API function statically with directly supplied request context
  remoteApi.hello("test")(using http) // String

  // Call the remote API function dynamically with directly supplied request context
  client.call[String]("hello").args("message" -> "test")(using http) // String

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

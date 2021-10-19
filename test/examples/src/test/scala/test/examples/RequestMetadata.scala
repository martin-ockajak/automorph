package test.examples

import automorph.DefaultHttpServer.{Context => ServerContext}
import automorph.DefaultHttpClient.{Context => ClientContext}
import automorph.{DefaultHttpClient, DefaultHttpServer}
import java.net.URI

object RequestMetadata extends App {

  // Define server API type and create its instance
  class ServerApi {

    // Use HTTP request metadata context provided by the server message transport plugin
    def useMetadata(message: String)(implicit request: ServerContext): String = Seq(
      Some(message),
      request.path,
      request.header("X-Test")
    ).flatten.mkString(",")
  }
  val api = new ServerApi()

  // Define client view of the server API
  trait ClientApi {

    // Use HTTP request metadata context defined by the client message transport plugin
    def useMetadata(message: String)(implicit request: ClientContext): String
  }

  // Start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = DefaultHttpServer.sync(_.bind(api), 80, "/api")

  // Create RPC client sending HTTP POST requests to 'http://localhost/api'
  val client = DefaultHttpClient.sync(new URI("http://localhost/api"), "POST")

  // Create client request context specifying HTTP request meta-data
  val requestMetadata = client.context
    .parameters("test" -> "value")
    .headers("X-Test" -> "value", "Cache-Control" -> "no-cache")
    .cookies("Test" -> "value")
    .authorizationBearer("value")

  // Call the remote API function statically with request context supplied directly
  val remoteApi = client.bind[ClientApi] // Api
  remoteApi.useMetadata("test")(requestMetadata) // String

  // Call the remote API function statically with request context supplied implictly
  implicit val givenRequestMetadata: automorph.DefaultHttpClient.Context = requestMetadata
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

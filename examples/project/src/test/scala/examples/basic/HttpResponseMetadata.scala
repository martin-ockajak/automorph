package examples.basic

import automorph.Default.{ClientContext, ServerContext}
import automorph.transport.http.HttpContext
import automorph.{Contextual, Default}
import java.net.URI

object HttpResponseMetadata extends App {

  // Create server API instance
  class ServerApi {

    // Return HTTP response context consumed by the server message transport plugin
    def hello(message: String): Contextual[String, ServerContext] = Contextual(
      message,
      HttpContext().headers("X-Test" -> "value", "Cache-Control" -> "no-cache")
    )
  }
  val api = new ServerApi()

  // Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
  val serverBuilder = Default.serverBuilderSync(7000, "/api")
  val server = serverBuilder(_.bind(api))

  // Define client view of the server API
  trait ClientApi {

    // Return HTTP response context provided by the client message transport plugin
    def hello(message: String): Contextual[String, ClientContext]
  }

  // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
  val client = Default.clientSync(new URI("http://localhost:7000/api"))

  // Call the remote API function retrieving a result with HTTP response metadata
  val remoteApi = client.bind[ClientApi]
  val static = remoteApi.hello("test")
  println(static.result)
  println(static.context.header("X-Test"))

  // Call the remote API function dynamically retrieving a result with HTTP response metadata
  val dynamic = client.call[Contextual[String, ClientContext]]("hello").args("message" -> "test")
  println(dynamic.result)
  println(dynamic.context.header("X-Test"))

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class HttpResponseMetadata extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      HttpResponseMetadata.main(Array())
    }
  }
}

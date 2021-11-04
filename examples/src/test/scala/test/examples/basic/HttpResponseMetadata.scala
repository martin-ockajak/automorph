//package test.examples.basic
//
//import automorph.Default.{ClientContext, ServerContext}
//import automorph.transport.http.HttpContext
//import automorph.{Contextual, Default}
//import java.net.URI
//
//object HttpResponseMetadata extends App {
//
//  // Create server API instance
//  class ServerApi {
//
//    // Return HTTP response context consumed by the server message transport plugin
//    def hello(message: String): Contextual[String, ServerContext] = Contextual(
//      message,
//      HttpContext().headers("X-Test" -> "value", "Cache-Control" -> "no-cache")
//    )
//  }
//  val api = new ServerApi()
//
//  // Start default JSON-RPC HTTP server listening on port 8080 for requests to '/api'
//  val createServer = Default.serverSync(8080, "/api")
//  val server = createServer(_.bind(api))
//
//  // Define client view of the server API
//  trait ClientApi {
//
//    // Return HTTP response context provided by the client message transport plugin
//    def hello(message: String): Contextual[String, ClientContext]
//  }
//
//  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
//  val client = Default.clientSync(new URI("http://localhost/api"))
//
//  // Call the remote API function expecting response context
//  val remoteApi = client.bind[ClientApi] // ClientApi
//  val static = remoteApi.hello("test") // Contextual[String, ClientContext]
//  static.result -> static.context.header("X-Test") // String -> "value"
//
//  // Call the remote API function dynamically expecting response context
//  val dynamic = client
//    .call[Contextual[String, ClientContext]]("hello")
//    .args("message" -> "test") // Contextual[String, ClientContext]
//  dynamic.result -> dynamic.context.header("X-Test") // String -> "value"
//
//  // Close the client
//  client.close()
//
//  // Stop the server
//  server.close()
//}
//
//class HttpResponseMetadata extends org.scalatest.freespec.AnyFreeSpecLike {
//  "" - {
//    "Test" ignore {
//      HttpResponseMetadata.main(Array())
//    }
//  }
//}

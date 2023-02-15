package examples.basic

import automorph.Default
import automorph.Default.{ClientContext, ServerContext}
import java.net.URI

object AccessControl extends App {

  // Create server API instance
  class ServerApi {

    // Accept HTTP request context provided by the server message transport plugin
    def hello(message: String)(implicit httpRequest: ServerContext): String =
      httpRequest.authorizationBearer match {
        case Some("valid") => s"Hello $message!"
        case _ => throw IllegalAccessException("Access denied")
      }
  }
  val api = new ServerApi()

  // Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
  val createServer = Default.serverBuilderSync(7000, "/api")
  val server = createServer(_.bind(api))

  // Define client view of a remote API
  trait ClientApi {

    // Accept HTTP request context consumed by the client message transport plugin
    def hello(message: String)(implicit http: ClientContext): String
  }

  // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
  val client = Default.clientSync(new URI("http://localhost:7000/api"))
  val remoteApi = client.bind[ClientApi]

  {
    // Create client request context specifying invalid HTTP authentication token
    implicit val validAuthentication: ClientContext = client.defaultContext
      .authorizationBearer("valid")

    // Call the remote API function statically using valid authentication
    println(
      remoteApi.hello("test")
    )

    // Call the remote API function dynamically using valid authentication
    println(
      client.call[String]("hello").args("message" -> "test")
    )
  }

  {
    // Create client request context specifying invalid HTTP authentication token
    implicit val invalidAuthentication: ClientContext = client.defaultContext
      .headers("X-Authentication" -> "unsupported")

    // Call the remote API function statically using invalid authentication
    println(Try(
      remoteApi.hello("test")
    ).failed.get)

    // Call the remote API function dynamically using invalid authentication
    println(Try(
      client.call[String]("hello").args("message" -> "test")
    ).failed.get)
  }

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class AccessControl extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      AccessControl.main(Array())
    }
  }
}

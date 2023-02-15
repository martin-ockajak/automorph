package examples.basic

import automorph.Default
import automorph.Default.{ClientContext, ServerContext}
import java.net.URI

object AccessControl extends App {

  // Create server API instance
  class ServerApi {

    // Accept HTTP request context provided by the server message transport plugin
    def hello(message: String)(implicit http: ServerContext): String =
      http.authorizationBearer match {
        case Some("valid") => s"Hello $message!"
        case _ => throw IllegalAccessException("Access denied")
      }
  }
  val api = new ServerApi()

  // Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
  val createServer = Default.serverSync(7000, "/api")
  val server = createServer(_.bind(api))

  // Define client view of a remote API
  trait ClientApi {

    // Accept HTTP request context consumed by the client message transport plugin
    def hello(message: String)(implicit http: ClientContext): String
  }

  // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
  val client = Default.clientSync(new URI("http://localhost:7000/api"))

  // Create client request context specifying HTTP request meta-data
  implicit val validAuthentication: ClientContext = client.defaultContext
    .authorizationBearer("valid")

  // Call the remote API function with implicitly given valid authentication token
  val remoteApi = client.bind[ClientApi]
  println(
    remoteApi.hello("test")
  )

  // Call the remote API function with directly supplied authentication token
  println(
    remoteApi.hello("test")(validAuthentication)
  )

  // Call the remote API function with directly supplied invalid authentication token
  println(
    remoteApi.hello("test")(
      client.defaultContext.authorizationBearer("invalid")
    )
  )

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

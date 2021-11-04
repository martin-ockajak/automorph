package test.examples.customize

import automorph.{Client, Default}
import java.net.URI
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ClientErrorMapping extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new ServerApi()

  // Start default JSON-RPC HTTP server listening on port 8080 for requests to '/api'
  val createServer = Default.serverAsync(8080, "/api")
  val server = createServer(_.bind(api))

  // Customize remote API client RPC error to exception mapping
  val protocol = Default.protocol.mapError((message, code) =>
    if (message.contains("SQL")) {
      new SQLException(message)
    } else {
      Default.protocol.mapError(message, code)
    }
  )

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): Future[String]
  }

  // Setup custom JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val transport = Default.clientTransportAsync(new URI("http://localhost/api"))
  val client = Client.protocol(protocol).transport(transport)

  // Call the remote API function
  val remoteApi = client.bind[ClientApi] // ClientApi
  remoteApi.hello("world", 1) // Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class ClientErrorMapping extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      ClientErrorMapping.main(Array())
    }
  }
}

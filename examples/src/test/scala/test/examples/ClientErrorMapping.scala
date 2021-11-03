package test.examples

import automorph.protocol.jsonrpc.ErrorType.InvalidRequest
import automorph.{Client, Default}
import java.net.URI
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ClientErrorMapping extends App {

  // Define an API type and create its instance
  class Api {

    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new Api()

  // Start default JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val createServer = Default.serverAsync(80, "/api")
  val server = createServer(_.bind(api))

  // Customize remote API client RPC error to exception mapping
  val protocol = Default.protocol.mapError {
    case (message, InvalidRequest.code) if message.contains("SQL") =>
      new SQLException(message)
    case (message, code) => Default.protocol.errorToException(message, code)
  }

  // Setup custom JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val transport = Default.clientTransportAsync(new URI("http://localhost/api"))
  val client = Client.protocol(protocol).transport(transport)

  // Call the remote API function
  val remoteApi = client.bind[Api] // Api
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

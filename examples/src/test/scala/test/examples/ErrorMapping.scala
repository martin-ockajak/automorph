package test.examples

import automorph.protocol.jsonrpc.ErrorType.InvalidRequest
import automorph.transport.http.HttpContext
import automorph.{Client, Default, Handler}
import java.net.URI
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ErrorMapping extends App {

  // Define an API type and create its instance
  class Api {
    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new Api()

  // Customize server exception to RPC error mapping
  val protocol = Default.protocol
  val serverProtocol = protocol.mapException {
    case _: SQLException => InvalidRequest
    case e => protocol.exceptionToError(e)
  }

  // Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val system = Default.systemAsync
  val handler = Handler.protocol(serverProtocol).system(system).context[Default.ServerContext]
  val server = Default.server(handler, 80, "/api", mapException = {
    // Customize server exception to HTTP status code mapping
    case _: SQLException => 400
    case e => HttpContext.defaultExceptionToStatusCode(e)
  })

  // Customize client RPC error to exception mapping
  val clientProtocol = protocol.mapError {
    case (message, InvalidRequest.code) if message.contains("SQL") =>
      new SQLException(message)
    case (message, code) => protocol.errorToException(message, code)
  }

  // Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val transport = Default.clientTransportAsync(new URI("http://localhost/api"), "POST")
  val client = Client.protocol(clientProtocol).transport(transport)

  // Call the remote API function
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 1) // Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class ErrorMapping extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      ErrorMapping.main(Array())
    }
  }
}

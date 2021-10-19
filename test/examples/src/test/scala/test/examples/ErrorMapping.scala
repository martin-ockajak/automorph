package test.examples

import automorph.protocol.jsonrpc.ErrorType.InvalidRequest
import automorph.transport.http.Http
import automorph.{Client, Default, Handler}
import java.net.URI
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ErrorMapping extends App {

  // Define an API type and create its instance
  class Api {
    def hello(some: String, n: Int): Future[String] = Future(s"Hello $some $n!")
  }
  val api = new Api()

  // Customize server RPC error mapping
  val protocol = Default.protocol
  val serverProtocol = protocol.mapException {
    case _: SQLException => InvalidRequest
    case e => protocol.exceptionToError(e)
  }

  // Start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
  val system = Default.asyncSystem
  val handler = Handler.protocol(serverProtocol).system(system).context[Default.HttpServerContext]
  val server = Default.httpServer(handler, (_: Future[Any]) => (), 80, "/api", {
    // Customize server HTTP status code mapping
    case _: SQLException => 400
    case e => Http.defaultExceptionToStatusCode(e)
  })

  // Customize client RPC error mapping
  val clientProtocol = protocol.mapError {
    case (message, InvalidRequest.code) if message.contains("SQL") => new SQLException(message)
    case (message, code) => protocol.errorToException(message, code)
  }

  // Create JSON-RPC client sending HTTP POST requests to 'http://localhost/api'
  val transport = Default.asyncHttpClientTransport(new URI("http://localhost/api"), "POST")
  val client = Client.protocol(clientProtocol).transport(transport)

  // Call the remote API function
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 1) // Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class ErrorMapping extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      ErrorMapping.main(Array())
    }
  }
}

package test.examples

import automorph.protocol.jsonrpc.ErrorType.InvalidRequest
import automorph.transport.http.Http
import automorph.{Client, DefaultEffectSystem, DefaultHttpClient, DefaultHttpClientTransport, DefaultHttpServer, DefaultRpcProtocol, Handler}
import java.net.URI
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ErrorMapping extends App {

  // Define an API type and create API instance
  class Api {
    def hello(some: String, n: Int): Future[String] = Future(s"Hello $some $n!")
  }
  val api = new Api()

  // Customize server RPC error mapping
  val defaultProtocol = DefaultRpcProtocol()
  val serverProtocol = defaultProtocol.exceptionToError {
    case _: SQLException => InvalidRequest
    case e => defaultProtocol.exceptionToError(e)
  }

  // Start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val system = DefaultEffectSystem.async
  val handler = Handler.protocol(serverProtocol).system(system).context[DefaultHttpServer.Context]
  val server = DefaultHttpServer(handler, (_: Future[Any]) => (), 80, "/api", {
    // Customize server HTTP status code mapping
    case _: SQLException => 400
    case e => Http.defaultExceptionToStatusCode(e)
  })

  // Customize client RPC error mapping
  val clientProtocol = defaultProtocol.errorToException {
    case (message, InvalidRequest.code) if message.contains("SQL") => new SQLException(message)
    case (message, code) => defaultProtocol.errorToException(message, code)
  }

  // Create RPC client sending HTTP POST requests to 'http://localhost/api'
  val transport = DefaultHttpClientTransport.async(new URI("http://localhost/api"), "POST")
  val client = Client.builder.protocol(clientProtocol).transport(transport).build

  // Call the remote API method via proxy
  val apiProxy = client.bind[Api] // Api
  apiProxy.hello("world", 1) // Future[String]

  // Call a remote API method dynamically passing the arguments by name
  val hello = client.method("hello")
  hello.args("some" -> "world", "n" -> 1).call[String] // Future[String]

  // Call a remote API method dynamically passing the arguments by position
  hello.positional.args("world", 1).call[String] // Future[String]

  // Notify a remote API method dynamically passing the arguments by name
  hello.args("some" -> "world", "n" -> 1).tell // Future[Unit]

  // Notify a remote API method dynamically passing the arguments by position
  hello.positional.args("world", 1).tell // Future[Unit]

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

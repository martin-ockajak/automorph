package test.examples

import automorph.protocol.jsonrpc.ErrorType.InvalidRequest
import automorph.protocol.jsonrpc.JsonRpcProtocol
import automorph.transport.http.Http
import automorph.{DefaultHttpClient, DefaultHttpServer}
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import automorph.transport.http.client.SttpClient.defaultContext

object ErrorMapping extends App {

  // Define an API type and create API instance
  class Api {
    def hello(some: String, n: Int): Future[String] = Future(s"Hello $some $n!")
  }
  val api = new Api()

  // Customize server JSON-RPC error mapping
  val exceptionToError = (exception: Throwable) =>
    exception match {
      case _: SQLException => InvalidRequest
      case e => JsonRpcProtocol.defaultExceptionToError(e)
    }
  val serverProtocol = JsonRpcProtocol().errorMapping(exceptionToError)

  // Customize server HTTP status code mapping
  val exceptionToStatusCode = (exception: Throwable) =>
    exception match {
      case _: SQLException => 400
      case e => Http.defaultExceptionToStatusCode(e)
    }

  // Start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = DefaultHttpServer.async(_.bind(api).protocol(serverProtocol), 80, "/api", exceptionToStatusCode)

  // Customize client JSON-RPC error mapping
  val errorToException = (message: String, code: Int) =>
    if (code == InvalidRequest.code && message.toUpperCase.contains("SQL")) {
      new SQLException(message)
    } else {
      JsonRpcProtocol.defaultErrorToException(message, code)
    }
  val clientProtocol = JsonRpcProtocol().errorMapping(errorToException)

  // Create RPC client for sending HTTP POST requests to 'http://localhost/api'
  val url = new java.net.URI("http://localhost/api")
  val client = DefaultHttpClient.async(url, "POST").protocol(clientProtocol)

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

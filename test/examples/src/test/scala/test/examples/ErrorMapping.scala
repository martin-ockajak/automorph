package test.examples

import automorph.protocol.ErrorType
import automorph.transport.http.client.SttpClient.defaultContext
import automorph.{Client, DefaultHttpClient, DefaultHttpServer, Handler}
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ErrorMapping extends App {

  // Define an API type and create API instance
  class Api {
    def hello(some: String, n: Int): Future[String] = Future(s"Hello $some $n!")
  }
  val api = new Api()

  // Customize default server error mapping
  val exceptionToError = (exception: Throwable) =>
    Handler.defaultErrorMapping(exception) match {
      case ErrorType.ApplicationError if exception.isInstanceOf[SQLException] => ErrorType.InvalidRequest
      case error => error
    }

  // Start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = DefaultHttpServer.async(_.bind(api).errorMapping(exceptionToError), 80, "/api")

  // Customize default client error mapping
  val errorToException = (code: Int, message: String) =>
    Client.defaultErrorMapping(code, message) match {
      case _: ErrorType.InvalidRequestException if message.toUpperCase.contains("SQL") => new SQLException(message)
      case exception => exception
    }

  // Create RPC client for sending HTTP POST requests to 'http://localhost/api'
  val url = new java.net.URI("http://localhost/api")
  val client = DefaultHttpClient.async(url, "POST").errorMapping(errorToException)

  // Call the remote API method via proxy
  val apiProxy = client.bind[Api] // Api
  apiProxy.hello("world", 1) // : Future[String]

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

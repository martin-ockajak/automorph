package examples

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import automorph.DefaultHttpClient
import automorph.DefaultHttpServer

object RequestContext extends App {

  // Define an API type and create API instance
  class ServerApi {
    import DefaultHttpServer.Context

    def requestMetaData(message: String)(implicit context: Context): Future[List[String]] = Future.successful(
      List(message, context.getRequestPath, context.getRequestHeaders.get("X-Test").peek)
    )
  }
  trait ClientApi {
    import DefaultHttpClient.Context

    def requestMetaData(message: String)(implicit context: Context): Future[List[String]]
  }
  val api = new ServerApi()

  // Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = DefaultHttpServer.async(_.bind(api), 80, "/api")

  // Create JSON-RPC client for sending HTTP POST requests to 'http://localhost/api'
  val client = DefaultHttpClient.async("http://localhost/api", "POST")

  // Call the remote API method via proxy
  val apiProxy = client.bind[ClientApi] // Api
  val defaultContext = client.defaultContext
  implicit val context: DefaultHttpClient.Context = defaultContext.copy(partial = defaultContext.partial.header("X-Test", "valid"))
  apiProxy.requestMetaData("test") // : Future(List("test", "/api", "valid"))
  apiProxy.requestMetaData("test")(context) // : Future(List("test", "/api", "valid"))

  // Stop the server
  server.close()
}

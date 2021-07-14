package examples

import automorph.codec.json.CirceJsonCodec
import automorph.transport.http.endpoint.UndertowJsonRpcHandler
import automorph.transport.http.server.UndertowServer
import automorph.{Client, DefaultBackend, DefaultHttpTransport, Handler}
import io.circe.generic.auto._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CustomMessageFormat extends App {

  // Define an API type and create API instance
  case class Record(values: List[String])
  class Api {
    def hello(some: String, n: Int): Future[Record] = Future.successful(Record(List("Hello", some, n.toString)))
  }
  val api = new Api()

  // Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
  val backend = DefaultBackend.async
  val runEffect = (effect: Future[_]) => effect
  val codec = CirceJsonCodec()
  val handler = Handler[CirceJsonCodec.Node, codec.type, Future, UndertowJsonRpcHandler.Context](codec, backend)
  val server = UndertowServer(UndertowJsonRpcHandler(handler.bind(api), runEffect), 80, "/api")

  // Create JSON-RPC client for sending HTTP POST requests to 'http://localhost/api'
  val clientTransport = DefaultHttpTransport.async("http://localhost/api", "POST")
  val client: Client[CirceJsonCodec.Node, codec.type, Future, DefaultHttpTransport.Context] =
    Client(codec, backend, clientTransport)

  // Call the remote API method via proxy
  val apiProxy = client.bind[Api] // Api
  apiProxy.hello("world", 1) // : Future[String]

  // Stop the server
  server.close()
}

package test.examples

import automorph.format.json.CirceJsonFormat
import automorph.transport.http.endpoint.UndertowHttpEndpoint
import automorph.transport.http.server.UndertowServer
import automorph.{Client, DefaultEffectSystem, DefaultHttpClientTransport, Handler}
import io.circe.generic.auto._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ChooseMessageFormat extends App {

  // Define an API type and create API instance
  case class Record(values: List[String])
  class Api {
    def hello(some: String, n: Int): Future[Record] = Future.successful(Record(List("Hello", some, n.toString)))
  }
  val api = new Api()

  // Create and start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val system = DefaultEffectSystem.async
  val runEffect = (effect: Future[_]) => effect
  val format = CirceJsonFormat()
  val handler = Handler[CirceJsonFormat.Node, format.type, Future, UndertowHttpEndpoint.Context](format, system)
  val server = UndertowServer(UndertowHttpEndpoint(handler.bind(api), runEffect), 80, "/api")

  // Create RPC client for sending HTTP POST requests to 'http://localhost/api'
  val transport = DefaultHttpClientTransport.async("http://localhost/api", "POST")
  val client: Client[CirceJsonFormat.Node, format.type, Future, DefaultHttpClientTransport.Context] =
    Client(format, system, transport)

  // Call the remote API method via proxy
  val apiProxy = client.bind[Api] // Api
  apiProxy.hello("world", 1) // : Future[String]

  // Stop the server
  server.close()
}

class ChooseMessageFormat extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      ChooseMessageFormat.main(Array())
    }
  }
}

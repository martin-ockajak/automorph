package test.examples

import automorph.format.messagepack.UpickleMessagePackFormat
import automorph.{Client, DefaultEffectSystem, DefaultHttpClientTransport, DefaultHttpServer, Handler}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MessageFormat extends App {

  // Define an API type and create API instance
  case class Record(values: List[String])
  class Api {
    def hello(some: String, n: Int): Future[Record] = Future(Record(List("Hello", some, n.toString)))
  }
  val api = new Api()

  // Create message format and custom data type serializer/deserializer
  val format = UpickleMessagePackFormat()
  implicit def recordRw: format.custom.ReadWriter[Record] = format.custom.macroRW

  // Create an effect system plugin
  val system = DefaultEffectSystem.async

  // Start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val handler = Handler[UpickleMessagePackFormat.Node, format.type, Future, DefaultHttpServer.Context](format, system)
  val server = DefaultHttpServer(handler.bind(api), identity, 80, "/api")

  // Create RPC client for sending HTTP POST requests to 'http://localhost/api'
  val url = new java.net.URI("http://localhost/api")
  val transport = DefaultHttpClientTransport.async(url, "POST")
  val client = Client[UpickleMessagePackFormat.Node, format.type, Future, DefaultHttpClientTransport.Context](format, system, transport)

  // Call the remote API method via proxy
  val apiProxy = client.bind[Api] // Api
  apiProxy.hello("world", 1) // : Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class MessageFormat extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      MessageFormat.main(Array())
    }
  }
}

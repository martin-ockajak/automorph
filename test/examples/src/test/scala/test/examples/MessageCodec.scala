package test.examples

import automorph.codec.messagepack.{UpickleMessagePackCodec}
import automorph.{Client, DefaultEffectSystem, DefaultHttpClientTransport, DefaultHttpServer, DefaultRpcProtocol, Handler}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MessageCodec extends App {

  // Define an API type and create API instance
  case class Record(values: List[String])

  class Api {
    def hello(some: String, n: Int): Future[Record] = Future(Record(List("Hello", some, n.toString)))
  }
  val api = new Api()

  // Create message codec and custom data type serializer/deserializer
  val codec = UpickleMessagePackCodec()
  implicit def recordRw: codec.custom.ReadWriter[Record] = codec.custom.macroRW
  val protocol = DefaultRpcProtocol[UpickleMessagePackCodec.Node, codec.type](codec)

  // Create an effect system plugin
  val system = DefaultEffectSystem.async

  // Start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val handler = Handler.protocol(protocol).system(system).context[DefaultHttpServer.Context]
  val server = DefaultHttpServer(handler.bind(api), (_: Future[Any]) => (), 80, "/api")

  // Create RPC client sending HTTP POST requests to 'http://localhost/api'
  val transport = DefaultHttpClientTransport.async(new URI("http://localhost/api"), "POST")
  val client = Client(protocol, transport)

  // Call the remote API method via proxy
  val apiProxy = client.bind[Api] // Api
  apiProxy.hello("world", 1) // : Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class MessageCodec extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      MessageCodec.main(Array())
    }
  }
}

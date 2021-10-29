package test.examples

import automorph.codec.messagepack.UpickleMessagePackCodec
import automorph.{Client, Default, Handler}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MessageCodec extends App {

  // Define an API type and create its instance
  case class Record(values: List[String])

  class Api {
    def hello(some: String, n: Int): Future[Record] =
      Future(Record(List("Hello", some, n.toString)))
  }
  val api = new Api()

  // Create uPickle message codec for JSON format
  val codec = UpickleMessagePackCodec()

  // Create custom data type serializer/deserializer
  implicit def recordRw: codec.custom.ReadWriter[Record] = codec.custom.macroRW

  // Create an RPC protocol plugin
  val protocol = Default.protocol[UpickleMessagePackCodec.Node, codec.type](codec)

  // Create an effect system plugin
  val system = Default.systemAsync

  // Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val handler = Handler.protocol(protocol).system(system).context[Default.ServerContext]
  val server = Default.server(handler.bind(api), 80, "/api")((_: Future[Any]) => ())

  // Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val transport = Default.clientTransportAsync(new URI("http://localhost/api"), "POST")
  val client = Client(protocol, transport)

  // Call the remote API function
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 1) // : Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class MessageCodec extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      MessageCodec.main(Array())
    }
  }
}

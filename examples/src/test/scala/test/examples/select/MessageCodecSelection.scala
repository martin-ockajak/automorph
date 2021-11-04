package test.examples.select

import automorph.codec.messagepack.UpickleMessagePackCodec
import automorph.{Client, Default, Handler}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MessageCodecSelection extends App {

  // Introduce custom data types
  case class Record(values: List[String])

  // Create uPickle message codec for JSON format
  val codec = UpickleMessagePackCodec()

  // Provide custom data type serialization and deserialization logic
  import codec.custom._
  implicit def recordRw: codec.custom.ReadWriter[Record] = codec.custom.macroRW

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): Future[Record] =
      Future(Record(List("Hello", some, n.toString)))
  }
  val api = new ServerApi()

  // Create an RPC protocol plugin
  val protocol = Default.protocol[UpickleMessagePackCodec.Node, codec.type](codec)

  // Create an effect system plugin
  val system = Default.systemAsync

  // Start default JSON-RPC HTTP server listening on port 8080 for requests to '/api'
  val handler = Handler.protocol(protocol).system(system).context[Default.ServerContext]
  lazy val server = Default.server(handler.bind(api), 8080, "/api")

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): Future[Record]
  }

  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val transport = Default.clientTransportAsync(new URI("http://localhost/api"))
  val client = Client(protocol, transport)

  // Call the remote API function
  val remoteApi = client.bind[ClientApi] // ClientApi
  remoteApi.hello("world", 1) // : Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class MessageCodecSelection extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      MessageCodecSelection.main(Array())
    }
  }
}
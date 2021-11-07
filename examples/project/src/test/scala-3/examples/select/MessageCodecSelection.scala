package examples.select

import automorph.codec.messagepack.UpickleMessagePackCodec
import automorph.{Client, Default, Handler}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object MessageCodecSelection extends App {

  // Introduce custom data types
  case class Record(values: List[String])

  // Create uPickle message codec for JSON format
  val codec = UpickleMessagePackCodec()

  // Provide custom data type serialization and deserialization logic
  import codec.custom.*
  implicit def recordRw: codec.custom.ReadWriter[Record] = codec.custom.macroRW

  // Create server API instance
  class ServerApi {

    def hello(some: String, n: Int): Future[Record] =
      Future(Record(List("Hello", some, n.toString)))
  }
  val api = new ServerApi()

  // Create a server RPC protocol plugin
  val serverProtocol =
    Default.protocol[UpickleMessagePackCodec.Node, codec.type, Default.ServerContext](
      codec
    )

  // Create an effect system plugin
  val system = Default.systemAsync

  // Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
  val handler = Handler.protocol(serverProtocol).system(system)
  lazy val server = Default.server(handler.bind(api), 7000, "/api")

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): Future[Record]
  }

  // Create a client RPC protocol plugin
  val clientProtocol =
    Default.protocol[UpickleMessagePackCodec.Node, codec.type, Default.ClientContext](
      codec
    )

  // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
  val transport = Default.clientTransportAsync(new URI("http://localhost:7000/api"))
  val client = Client(clientProtocol, transport)

  // Call the remote API function
  val remoteApi = client.bind[ClientApi] // ClientApi
  remoteApi.hello("world", 1) // : Future[String]

  // Close the client
  Await.result(client.close(), Duration.Inf)

  // Stop the server
  Await.result(server.close(), Duration.Inf)
}

class MessageCodecSelection extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      MessageCodecSelection.main(Array())
    }
  }
}

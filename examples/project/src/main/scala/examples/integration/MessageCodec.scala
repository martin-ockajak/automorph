package examples.integration

import automorph.codec.messagepack.{UpickleMessagePackCodec, UpickleMessagePackCustom}
import automorph.{Client, Default, Handler}

import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

// Introduce custom data types
private[examples] case class Record(values: List[String])

private[examples] object MessageCodec {

  @scala.annotation.nowarn
  def main(arguments: Array[String]): Unit = {

    // Create uPickle message codec for JSON format
    val messageCodec = UpickleMessagePackCodec[UpickleMessagePackCustom]()

    // Provide custom data type serialization and deserialization logic
    import messageCodec.custom.*
    implicit def recordRw: messageCodec.custom.ReadWriter[Record] = messageCodec.custom.macroRW[Record]

    // Create server API instance
    class ServerApi {

      def hello(some: String, n: Int): Future[Record] =
        Future(Record(List("Hello", some, n.toString)))
    }
    val api = new ServerApi()

    // Create a server RPC protocol plugin
    val serverProtocol = Default.rpcProtocol[UpickleMessagePackCodec.Node, messageCodec.type, Default.ServerContext](messageCodec)

    // Create an effect system plugin
    val effectSystem = Default.effectSystemAsync

    // Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
    val handler = Handler.protocol(serverProtocol).system(effectSystem)
    val server = Default.server(handler.bind(api), 7000, "/api")

    // Define client view of the remote API
    trait ClientApi {
      def hello(some: String, n: Int): Future[Record]
    }

    // Create a client RPC protocol plugin
    val clientProtocol = Default.rpcProtocol[UpickleMessagePackCodec.Node, messageCodec.type, Default.ClientContext](messageCodec)

    // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
    val clientTransport = Default.clientTransportAsync(new URI("http://localhost:7000/api"))
    val client = Client(clientProtocol, clientTransport)

    // Call the remote API function
    val remoteApi = client.bind[ClientApi]
    println(Await.result(
      remoteApi.hello("world", 1),
      Duration.Inf
    ))

    // Close the client
    Await.result(client.close(), Duration.Inf)

    // Stop the server
    Await.result(server.close(), Duration.Inf)
  }
}

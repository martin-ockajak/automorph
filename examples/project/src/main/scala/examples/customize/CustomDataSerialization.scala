package examples.customize

import automorph.Default
import io.circe.{Decoder, Encoder}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

private[examples] object CustomDataSerialization {
  @scala.annotation.nowarn
  def main(arguments: Array[String]): Unit = {

    // Introduce custom data types
    sealed abstract class State

    object State {
      case object On extends State
      case object Off extends State
    }

    case class Record(
      value: String,
      state: State
    )

    // Provide custom data type serialization and deserialization logic
    import io.circe.generic.auto.*
    implicit lazy val enumEncoder: Encoder[State] = Encoder.encodeInt.contramap[State](Map(
      State.Off -> 0,
      State.On -> 1
    ))
    implicit lazy val enumDecoder: Decoder[State] = Decoder.decodeInt.map(Map(
      0 -> State.Off,
      1 -> State.On
    ))

    // Create server API instance
    class ServerApi {
      def hello(some: String, record: Record): Future[Record] =
        Future(record.copy(value = s"Hello $some!"))
    }
    val api = new ServerApi()

    // Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
    val serverBuilder = Default.serverBuilderAsync(7000, "/api")
    val server = serverBuilder(_.bind(api))

    // Define client view of the remote API
    trait ClientApi {
      def hello(some: String, record: Record): Future[Record]
    }

    // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
    val client = Default.clientAsync(new URI("http://localhost:7000/api"))

    // Call the remote API function via proxy
    lazy val remoteApi = client.bind[ClientApi]
    println(Await.result(
      remoteApi.hello("world", Record("test", State.On)),
      Duration.Inf
    ))

    // Close the client
    Await.result(client.close(), Duration.Inf)

    // Stop the server
    Await.result(server.close(), Duration.Inf)
  }
}

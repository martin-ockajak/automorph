package test.examples.customize

import automorph.Default
import io.circe.{Decoder, Encoder}
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CustomDataSerialization extends App {

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
  import io.circe.generic.auto._
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
    def hello(some: String, n: Int, record: Record): Future[Record] =
      Future(record.copy(value = s"Hello $some $n!"))
  }
  val api = new ServerApi()

  // Start default JSON-RPC HTTP server listening on port 8080 for requests to '/api'
  val createServer = Default.serverAsync(8080, "/api")
  lazy val server = createServer(_.bind(api))

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int, record: Record): Future[Record]
  }

  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.clientAsync(new URI("http://localhost/api"))

  // Call the remote API function via proxy
  lazy val remoteApi = client.bind[ClientApi] // ClientApi
  remoteApi.hello("world", 1, Record("test", State.On)) // Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class CustomDataSerialization extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      CustomDataSerialization.main(Array())
    }
  }
}

package examples.metadata

import automorph.Default
import io.circe.Json
import java.net.URI

private[examples] object DynamicPayload {
  @scala.annotation.nowarn
  def main(arguments: Array[String]): Unit = {

    // Create server API instance
    class ServerApi {
      def hello(some: Json, n: Int): Json =
        if (some.isString) {
          val value = some.as[String].toTry.get
          Json.fromString(s"Hello $value $n!")
        } else {
          Json.fromValues(Seq(some, Json.fromInt(n)))
        }
    }
    val api = new ServerApi()

    // Start JSON-RPC HTTP server listening on port 7000 for PUT requests to '/api'
    val serverBuilder = Default.serverBuilderSync(7000, "/api")
    val server = serverBuilder(_.bind(api))

    // Define client view of the remote API
    trait ClientApi {
      def hello(some: String, n: Json): Json
    }

    // Setup JSON-RPC HTTP client sending PUT requests to 'http://localhost:7000/api'
    val client = Default.clientSync(new URI("http://localhost:7000/api"))

    // Call the remote API function statically
    val remoteApi = client.bind[ClientApi]
    println(
      remoteApi.hello("world", Json.fromInt(1))
    )

    // Call the remote API function dynamically
    println(
      client.call[Seq[Int]]("hello").args("some" -> Json.fromInt(0), "n" -> 1)
    )

    // Close the client
    client.close()

    // Stop the server
    server.close()
  }
}

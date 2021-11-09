package examples.basic

import automorph.Default
import automorph.transport.http.HttpMethod
import java.net.URI

object OptionalParameters extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Option[Int]): String =
      s"Hello $some ${n.getOrElse(0)}!"

    def hi(some: Option[String])(n: Int): String =
      s"Hi ${some.getOrElse("all")} $n!"
  }
  val api = new ServerApi()

  // Start JSON-RPC HTTP server listening on port 7000 for PUT requests to '/api'
  val createServer = Default.serverSync(7000, "/api", Seq(HttpMethod.Put))
  val server = createServer(_.bind(api))

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String): String
  }
  // Setup JSON-RPC HTTP client sending PUT requests to 'http://localhost:7000/api'
  val client = Default.clientSync(new URI("http://localhost:7000/api"), HttpMethod.Put)

  // Call the remote API function statically
  val remoteApi = client.bind[ClientApi] // ClientApi
  remoteApi.hello("world") // String

  // Call the remote API function dynamically
  client.call[String]("hi").args("n" -> 1) // String

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class OptionalParameters extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      SynchronousCall.main(Array())
    }
  }
}

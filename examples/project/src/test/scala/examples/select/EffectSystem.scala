package examples.select

import automorph.Default
import automorph.system.ZioSystem
import java.net.URI
import zio.{Runtime, Task}

object EffectSystem extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): Task[String] =
      Task.succeed(s"Hello $some $n!")
  }
  val api = new ServerApi()

  // Create ZIO effect system plugin
  val system = ZioSystem.default

  // Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
  val server = Default.serverSystem(system, 7000, "/api")(_.bind(api))

  // Define client view of the remote API
  trait ClientApi {
    def hello(some: String, n: Int): Task[String]
  }

  // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
  val client = Default.client(system, new URI("http://localhost:7000/api"))

  // Call the remote APi function via proxy
  val remoteApi = client.bind[ClientApi]
  println(Runtime.default.unsafeRunTask(
    remoteApi.hello("world", 1)
  ))

  // Close the client
  Runtime.default.unsafeRunTask(client.close())

  // Stop the server
  Runtime.default.unsafeRunTask(server.close())
}

class EffectSystem extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      EffectSystem.main(Array())
    }
  }
}

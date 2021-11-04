package test.examples.select

import automorph.Default
import automorph.system.ZioSystem
import java.net.URI
import zio.Task

object EffectSystemSelection extends App {

  // Define an API and create its instance
  class Api {

    def hello(some: String, n: Int): Task[String] =
      Task(s"Hello $some $n!")
  }
  val api = new Api()

  // Create ZIO effect system plugin
  val system = ZioSystem.default

  // Start default JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val server = Default.serverSystem(system, 80, "/api")(_.bind(api))

  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.client(system, new URI("http://localhost/api"))

  // Call the remote APi function via proxy
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 1) // Task[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class EffectSystemSelection extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      EffectSystemSelection.main(Array())
    }
  }
}

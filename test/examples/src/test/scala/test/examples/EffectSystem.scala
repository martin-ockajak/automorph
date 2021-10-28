package test.examples

import automorph.Default
import automorph.system.ZioSystem
import java.net.URI
import org.asynchttpclient.DefaultAsyncHttpClient
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.Runtime.default.unsafeRunTask
import zio.{Runtime, Task}

object EffectSystem extends App {

  // Define an API and create its instance
  class Api {
    def hello(some: String, n: Int): Task[String] = Task(s"Hello $some $n!")
  }
  val api = new Api()

  // Create ZIO effect system plugin
  val system = ZioSystem[Any]()

  // Start Undertow JSON-RPC HTTP server listening on port 80 for requests to '/api'
  val server = Default.serverSystem(system, unsafeRunTask, _.bind(api), 80, "/api")

  // Setup STTP JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val backend = AsyncHttpClientZioBackend.usingClient(Runtime.default, new DefaultAsyncHttpClient())
  val client = Default.client(new URI("http://localhost/api"), "POST", backend, system)

  // Call the remote APi function via proxy
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 1) // Task[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class EffectSystem extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      EffectSystem.main(Array())
    }
  }
}

package test.examples

import automorph.system.ZioSystem
import automorph.{DefaultHttpClient, DefaultHttpServer}
import java.net.URI
import org.asynchttpclient.DefaultAsyncHttpClient
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.{Runtime, Task}
import zio.Runtime.default.unsafeRunTask

object EffectSystem extends App {

  // Define an API and create its instance
  class Api {
    def hello(some: String, n: Int): Task[String] = Task(s"Hello $some $n!")
  }
  val api = new Api()

  // Create an effect system plugin
  val system = ZioSystem[Any]()

  // Start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = DefaultHttpServer.system(system, unsafeRunTask, _.bind(api), 80, "/api")

  // Create RPC client sending HTTP POST requests to 'http://localhost/api'
  val backend = AsyncHttpClientZioBackend.usingClient(Runtime.default, new DefaultAsyncHttpClient())
  val client = DefaultHttpClient(new URI("http://localhost/api"), "POST", backend, system)

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

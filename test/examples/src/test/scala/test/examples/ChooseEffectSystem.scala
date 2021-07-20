package test.examples

import automorph.system.ZioSystem
import automorph.{DefaultHttpClient, DefaultHttpServer}
import org.asynchttpclient.DefaultAsyncHttpClient
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.{Runtime, Task}

object ChooseEffectSystem extends App {

  // Define an API type and create API instance
  class Api {
    def hello(some: String, n: Int): Task[String] = Task.succeed(s"Hello $some $n!")
  }
  val api = new Api()

  // Create effect system plugin
  val system = ZioSystem[Any]()
  val runEffect = (effect: Task[_]) => Runtime.default.unsafeRunTask(effect)

  // Create and start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = DefaultHttpServer.system[ZioSystem.TaskEffect](system, runEffect, _.bind(api), 80, "/api")

  // Create RPC client for sending HTTP POST requests to 'http://localhost/api'
  val url = new java.net.URI("http://localhost/api")
  val backend = AsyncHttpClientZioBackend.usingClient(Runtime.default, new DefaultAsyncHttpClient())
  val client = DefaultHttpClient(url, "POST", system, backend)

  // Call the remote API method via proxy
  val apiProxy = client.bind[Api] // Api
  apiProxy.hello("world", 1) // : Task[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class ChooseEffectSystem extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      ChooseEffectSystem.main(Array())
    }
  }
}

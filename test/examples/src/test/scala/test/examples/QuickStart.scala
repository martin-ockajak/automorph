package test.examples

import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object QuickStart extends App {

  // Define an API
  trait Api {
    def hello(some: String, n: Int): Future[String]
  }

  // Create the API instance
  class ApiImpl extends Api {
    override def hello(some: String, n: Int): Future[String] = Future(s"Hello $some $n!")
  }
  val api = new ApiImpl()

  // Start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = Default.asyncHttpServer(_.bind(api), 80, "/api")

  // Create JSON-RPC client sending HTTP POST requests to 'http://localhost/api'
  val client = Default.asyncHttpClient(new URI("http://localhost/api"), "POST")

  // Call the remote API function via proxy
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 3) // Future[String]

  // Call the remote API function dynamically
  val remoteHello = client.function("hello") // RemoteFunction
  remoteHello.args("what" -> "world", "n" -> 3).call[String] // Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class QuickStart extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      QuickStart.main(Array())
    }
  }
}

package test.examples

import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object QuickStart extends App {

  // Define an API
  trait Api {
    def hello(what: String, n: Int): Future[String]
  }

  // Create the API instance
  class ApiImpl extends Api {
    override def hello(what: String, n: Int): Future[String] = Future(s"Hello $n $what!")
  }
  val api = new ApiImpl()

  // Start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = Default.asyncHttpServer(_.bind(api), 80, "/api")

  // Create RPC client sending HTTP POST requests to 'http://localhost/api'
  val client = Default.asyncHttpClient(new URI("http://localhost/api"), "POST")

  // Call the remote API function via proxy
  val apiProxy = client.bind[Api] // Api
  apiProxy.hello("world", 3) // Future[String]

  // Call the remote API function dynamically
  val hello = client.function("hello")
  hello.args("what" -> "world", "n" -> 3).call[String] // Future[String]

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

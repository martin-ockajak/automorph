package test.examples

import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Asynchronous extends App {

  // Define an API type and create API instance
  trait Api {
    def hello(what: String, n: Int): Future[String]
  }
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

  // Call a remote API function dynamically
  val hello = client.function("hello")
  hello.args("what" -> "world", "n" -> 3).call[String] // Future[String]

  // Notify a remote API function dynamically
  hello.args("what" -> "world", "n" -> 3).tell // Future[Unit]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class Asynchronous extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      Asynchronous.main(Array())
    }
  }
}

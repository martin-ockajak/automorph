package test.examples

object Synchronous extends App {

  // Define an API type and create API instance
  class Api {
    def hello(some: String, n: Int): String = s"Hello $some $n!"
  }
  val api = new Api()

  // Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = automorph.DefaultHttpServer.sync(_.bind(api), 80, "/api")

  // Create JSON-RPC client for sending HTTP POST requests to 'http://localhost/api'
  val client = automorph.DefaultHttpClient.sync("http://localhost/api", "POST")

  // Call the remote API method via proxy
  val apiProxy = client.bind[Api] // Api
  apiProxy.hello("world", 1) // : String

  // Stop the server
  server.close()
}

class Synchronous extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      Synchronous.main(Array())
    }
  }
}

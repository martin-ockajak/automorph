package test.examples

import automorph.system.IdentitySystem
import automorph.transport.http.client.HttpUrlConnectionClient
import automorph.{DefaultClient, DefaultHttpServer}

object ClientMessageTransport extends App {

  // Define an API type and create API instance
  class Api {
    def hello(some: String, n: Int): String = s"Hello $some $n!"
  }
  val api = new Api()

  // Start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = DefaultHttpServer.sync(_.bind(api), 80, "/api")

  // Create RPC client for sending HTTP POST requests to 'http://localhost/api'
  val url = new java.net.URI("http://localhost/api")
  val transport = HttpUrlConnectionClient(url, "POST", IdentitySystem())
  val client = DefaultClient.sync(transport)

  // Call the remote API method via proxy
  val apiProxy = client.bind[Api] // Api
  apiProxy.hello("world", 1) // : String

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class ClientMessageTransport extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      ClientMessageTransport.main(Array())
    }
  }
}

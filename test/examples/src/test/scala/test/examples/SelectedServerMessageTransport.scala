package test.examples

import automorph.transport.http.server.NanoHttpdServer
import automorph.{DefaultHandler, DefaultHttpClient}

object SelectedServerMessageTransport extends App {

  // Define an API type and create API instance
  class Api {
    def hello(some: String, n: Int): String = s"Hello $some $n!"
  }
  val api = new Api()

  // Start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val handler = DefaultHandler.sync[NanoHttpdServer.Context]
  val server = NanoHttpdServer(handler.bind(api), identity, 80)

  // Create RPC client for sending HTTP POST requests to 'http://localhost/api'
  val url = new java.net.URI("http://localhost/api")
  val client = DefaultHttpClient.sync(url, "POST")

  // Call the remote API method via proxy
  val apiProxy = client.bind[Api] // Api
  apiProxy.hello("world", 1) // : String

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class SelectedServerMessageTransport extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      SelectedServerMessageTransport.main(Array())
    }
  }
}

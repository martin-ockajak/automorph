package test.example

import automorph.transport.http.server.NanoHttpdServer
import automorph.{DefaultHandler, DefaultEffectSystem, DefaultHttpClient}
import java.net.URI

object ServerMessageTransport extends App {

  // Define an API type and create its instance
  class Api {
    def hello(some: String, n: Int): String = s"Hello $some $n!"
  }
  val api = new Api()

  // Start NanoHTPD JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
  val handler = DefaultHandler.sync[NanoHttpdServer.Context]
  val server = NanoHttpdServer(handler.bind(api), (response: DefaultEffectSystem.SyncEffect[NanoHttpdServer.Response]) => response, 80)

  // Create JSON-RPC client sending HTTP POST requests to 'http://localhost/api'
  val client = DefaultHttpClient.sync(new URI("http://localhost/api"), "POST")

  // Call the remote API function
  val remoteApi = client.bind[Api] // Api
  remoteApi.hello("world", 1) // String

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class ServerMessageTransport extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      ServerMessageTransport.main(Array())
    }
  }
}

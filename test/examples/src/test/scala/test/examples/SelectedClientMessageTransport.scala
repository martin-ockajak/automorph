package test.examples

import automorph.system.IdentitySystem.Identity
import automorph.transport.http.client.HttpUrlConnectionClient
import automorph.transport.http.server.NanoHttpdServer
import automorph.{DefaultClient, DefaultEffectSystem, DefaultHttpServer, DefaultMessageFormat}

object SelectedClientMessageTransport extends App {

  // Define an API type and create API instance
  class Api {
    def hello(some: String, n: Int): String = s"Hello $some $n!"
  }
  val api = new Api()

  // Create an effect system plugin
  val system = DefaultEffectSystem.sync

  // Start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val server = DefaultHttpServer.sync(_.bind(api), 80, "/api")

  // Create RPC client for sending HTTP POST requests to 'http://localhost/api'
  val url = new java.net.URI("http://localhost/api")
  val format = DefaultMessageFormat()
  val transport = HttpUrlConnectionClient(url, "POST")
  val client = DefaultClient(system, transport)

  // Call the remote API method via proxy
  val apiProxy = client.bind[Api] // Api
  apiProxy.hello("world", 1) // : String

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class SelectedClientMessageTransport extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      SelectedClientMessageTransport.main(Array())
    }
  }
}

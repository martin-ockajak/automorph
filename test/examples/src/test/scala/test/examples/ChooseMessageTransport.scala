package test.examples

import automorph.transport.http.server.NanoHttpdServer
import automorph.transport.http.client.UrlConnectionClient
import automorph.system.IdentitySystem.Identity
import automorph.{Client, DefaultEffectSystem, DefaultMessageFormat, Handler}
import java.net.URI

object ChooseMessageTransport extends App {

  // Define an API type and create API instance
  class Api {
    def hello(some: String, n: Int): String = s"Hello $some $n!"
  }
  val api = new Api()

  // Create and start RPC server listening on port 80 for HTTP requests with URL path '/api'
  val system = DefaultEffectSystem.sync
  val runEffect = (effect: Identity[NanoHttpdServer.Response]) => effect
  val format = DefaultMessageFormat()
  val handler = Handler[DefaultMessageFormat.Node, format.type, Identity, NanoHttpdServer.Context](format, system)
  val server = NanoHttpdServer(handler.bind(api), runEffect, 80)

  // Create RPC client for sending HTTP POST requests to 'http://localhost/api'
  val transport = UrlConnectionClient(new URI("http://localhost/api"), "POST")
  val client: Client[DefaultMessageFormat.Node, format.type, Identity, UrlConnectionClient.Context] =
    Client(format, system, transport)

  // Call the remote API method via proxy
  val apiProxy = client.bind[Api] // Api
  apiProxy.hello("world", 1) // : String

  // Stop the server
  server.close()
}

class ChooseMessageTransport extends test.base.BaseSpec {
  "" - {
    "Test" ignore {
      ChooseMessageTransport.main(Array())
    }
  }
}

package examples

import automorph.backend.IdentityBackend.Identity
import automorph.transport.http.server.NanoHttpdServer
import automorph.transport.http.client.UrlConnectionTransport
import automorph.{Client, DefaultBackend, DefaultCodec, Handler}
import java.net.URL

object SelectMessageTransport extends App {

  // Define an API type and create API instance
  class Api {
    def hello(some: String, n: Int): String = s"Hello $some $n!"
  }
  val api = new Api()

  // Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
  val backend = DefaultBackend.sync
  val runEffect = (effect: Identity[NanoHttpdServer.Response]) => effect
  val codec = DefaultCodec()
  val handler = Handler[DefaultCodec.Node, codec.type, Identity, NanoHttpdServer.Context](codec, backend)
  val server = NanoHttpdServer(handler.bind(api), runEffect, 80)

  // Create JSON-RPC client for sending HTTP POST requests to 'http://localhost/api'
  val transport = UrlConnectionTransport(new URL("http://localhost/api"), "POST")
  val client: Client[DefaultCodec.Node, codec.type, Identity, UrlConnectionTransport.Context] =
    Client(codec, backend, transport)

  // Call the remote API method via proxy
  val apiProxy = client.bind[Api] // Api
  apiProxy.hello("world", 1) // : String

  // Stop the server
  server.close()
}

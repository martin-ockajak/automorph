package test.standard

import automorph.Types
import automorph.spi.{ClientMessageTransport, ServerMessageTransport}
import automorph.transport.http.HttpMethod
import automorph.transport.http.client.HttpClient
import java.net.URI
import test.core.ClientServerTest

trait StandardHttpServerTest extends ClientServerTest {

  def serverTransport(handler: Types.HandlerAnyCodec[Effect, Context], port: Int): ServerMessageTransport[Effect, Context]

  override def clientTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] = {
    val (server, port) = withRandomAvailablePort(port => serverTransport(handler, port) -> port)
    servers += server
    val scheme = Option.when(webSocket)("ws").getOrElse("http")
    val url = new URI(s"$scheme://localhost:$port")
    val client = HttpClient(system, url, HttpMethod.Post).asInstanceOf[ClientMessageTransport[Effect, Context]]
    clients += client
    Some(client)
  }

  def webSocket: Boolean =
    false
}

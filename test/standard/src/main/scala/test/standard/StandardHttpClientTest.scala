package test.standard

import automorph.Types
import automorph.spi.transport.{ClientMessageTransport, ServerMessageTransport}
import automorph.transport.http.server.NanoServer
import java.net.URI
import test.core.ClientServerTest

trait StandardHttpClientTest extends ClientServerTest {

  def clientTransport(url: URI): ClientMessageTransport[Effect, Context]

  override def clientTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] = {
    val (server, port) = withRandomAvailablePort(port =>
      NanoServer.create[Effect](handler.asInstanceOf[Types.HandlerAnyCodec[Effect, NanoServer.Context]], port)(
        execute
      ).asInstanceOf[ServerMessageTransport[Effect, Context]] -> port
    )
    servers += server
    val scheme = Option.when(webSocket)("ws").getOrElse("http")
    val url = new URI(s"$scheme://localhost:$port")
    val client = clientTransport(url)
    clients += client
    Some(client)
  }

  def webSocket: Boolean =
    false
}

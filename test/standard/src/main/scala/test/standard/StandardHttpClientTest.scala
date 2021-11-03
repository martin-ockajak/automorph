package test.standard

import automorph.Types
import automorph.spi.transport.ClientMessageTransport
import automorph.transport.http.server.NanoServer
import java.net.URI
import test.core.ClientServerTest

trait StandardHttpClientTest extends ClientServerTest {

  def clientTransport(url: URI): ClientMessageTransport[Effect, Context]

  def webSocket: Boolean = false

  override def customTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] = {
    val server = withRandomAvailablePort(port =>
      NanoServer.create[Effect](
        handler.asInstanceOf[Types.HandlerAnyCodec[Effect, NanoServer.Context]],
        port
      )(run(_))
    )
    servers += server
    val scheme = Option.when(webSocket)("ws").getOrElse("http")
    val url = new URI(s"$scheme://localhost:${server.port}")
    val client = clientTransport(url)
    clients += client
    Some(client)
  }
}

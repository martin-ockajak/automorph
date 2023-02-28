package test.standard

import automorph.Types
import automorph.spi.{ClientTransport, ServerTransport}
import automorph.transport.http.server.NanoServer
import java.net.URI
import test.core.ClientServerTest

trait StandardHttpClientTest extends ClientServerTest {

  def clientTransport(url: URI): ClientTransport[Effect, Context]

  override def clientTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientTransport[Effect, Context]] = {
    val (server, port) = withRandomAvailablePort(port =>
      NanoServer[Effect](
        handler.asInstanceOf[Types.HandlerAnyCodec[Effect, NanoServer.Context]],
        port
      ).asInstanceOf[ServerTransport[Effect, Context]] -> port
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

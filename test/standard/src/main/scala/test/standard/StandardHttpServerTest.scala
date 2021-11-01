package test.standard

import automorph.Types
import automorph.spi.EffectSystem
import automorph.spi.system.Defer
import automorph.spi.transport.{ClientMessageTransport, ServerMessageTransport}
import automorph.transport.http.client.HttpClient
import java.net.URI
import test.core.ClientServerTest

trait StandardHttpServerTest extends ClientServerTest {

  override lazy val system: EffectSystem[Effect] = deferSystem

  def deferSystem: EffectSystem[Effect] with Defer[Effect]

  def serverTransport(handler: Types.HandlerAnyCodec[Effect, Context], port: Int): ServerMessageTransport[Effect]

  override def customTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] = {
    val (server, port) = withAvailablePort(port => serverTransport(handler, port) -> port)
    servers += server
    val url = new URI(s"http://localhost:$port")
    val client = HttpClient.create(url, "POST", deferSystem)(runEffect)
      .asInstanceOf[ClientMessageTransport[Effect, Context]]
    clients += client
    Some(client)
  }
}

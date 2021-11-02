package test.standard

import automorph.Types
import automorph.spi.transport.{ClientMessageTransport, ServerMessageTransport}
import automorph.transport.http.client.HttpClient
import java.net.URI
import test.core.ClientServerTest

trait StandardHttpServerTest extends ClientServerTest {

  def serverTransport(handler: Types.HandlerAnyCodec[Effect, Context], port: Int): ServerMessageTransport[Effect]

  def webSocket: Boolean = false

  override def customTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] = {
    val (server, port) = withAvailablePort(port => serverTransport(handler, port) -> port)
    servers += server
    val scheme = Option.when(webSocket)("ws").getOrElse("http")
    val url = new URI(s"$scheme://localhost:$port")
    val client = HttpClient.create(url, "POST", system)(runEffect)
      .asInstanceOf[ClientMessageTransport[Effect, Context]]
    clients += client
    Some(client)
  }
}

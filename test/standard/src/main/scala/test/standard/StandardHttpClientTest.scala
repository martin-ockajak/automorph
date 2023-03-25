package test.standard

import automorph.spi.ServerTransport
import automorph.transport.http.server.NanoServer
import test.core.ClientServerTest

trait StandardHttpClientTest extends ClientServerTest {

  override def serverTransport: ServerTransport[Effect, Context] =
    NanoServer[Effect](system, port).asInstanceOf[ServerTransport[Effect, Context]]

  def webSocket: Boolean =
    false
}

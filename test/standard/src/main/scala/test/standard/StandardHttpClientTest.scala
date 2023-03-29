package test.standard

import automorph.spi.ServerTransport
import automorph.transport.http.server.NanoServer
import test.core.ClientServerTest

trait StandardHttpClientTest extends ClientServerTest {

  override def serverTransport(id: Int): ServerTransport[Effect, Context] =
    NanoServer[Effect](system, port(id)).asInstanceOf[ServerTransport[Effect, Context]]
}

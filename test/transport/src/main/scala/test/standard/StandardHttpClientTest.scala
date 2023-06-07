package test.standard

import automorph.spi.ServerTransport
import automorph.transport.http.server.NanoServer
import test.core.ClientServerTest

trait StandardHttpClientTest extends ClientServerTest {

  override def serverTransport(fixtureId: Int): ServerTransport[Effect, Context] =
    NanoServer[Effect](system, port(fixtureId)).asInstanceOf[ServerTransport[Effect, Context]]
}

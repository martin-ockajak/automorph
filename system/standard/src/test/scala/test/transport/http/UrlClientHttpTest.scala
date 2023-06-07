package test.transport.http

import automorph.spi.{ClientTransport, ServerTransport}
import automorph.transport.http.HttpMethod
import automorph.transport.http.client.UrlClient
import automorph.transport.http.server.NanoServer
import org.scalacheck.Arbitrary
import test.core.ClientServerTest

trait UrlClientHttpTest extends ClientServerTest {

  type Context = NanoServer.Context

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def clientTransport(fixtureId: Int): ClientTransport[Effect, ?] =
    UrlClient(system, url(fixtureId), HttpMethod.Put)

  override def serverTransport(fixtureId: Int): ServerTransport[Effect, Context] =
    NanoServer[Effect](system, port(fixtureId))
}

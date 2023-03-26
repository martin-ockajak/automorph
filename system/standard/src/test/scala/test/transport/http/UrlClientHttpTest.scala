package test.transport.http

import automorph.spi.{ClientTransport, ServerTransport}
import automorph.transport.http.HttpMethod
import automorph.transport.http.client.UrlClient
import automorph.transport.http.server.NanoServer
import java.net.URI
import org.scalacheck.Arbitrary
import test.core.ClientServerTest

trait UrlClientHttpTest extends ClientServerTest {

  type Context = NanoServer.Context

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def clientTransport: ClientTransport[Effect, ?] =
    UrlClient(system, new URI(s"http://localhost:$port"), HttpMethod.Put)

  override def serverTransport: ServerTransport[Effect, Context] =
    NanoServer[Effect](system, port)
}

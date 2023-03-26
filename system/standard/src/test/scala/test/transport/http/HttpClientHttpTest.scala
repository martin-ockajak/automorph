package test.transport.http

import automorph.spi.{ClientTransport, ServerTransport}
import automorph.transport.http.HttpMethod
import automorph.transport.http.client.HttpClient
import automorph.transport.http.server.NanoServer
import java.net.URI
import org.scalacheck.Arbitrary
import test.core.ClientServerTest

trait HttpClientHttpTest extends ClientServerTest {

  type Context = NanoServer.Context

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def clientTransport: ClientTransport[Effect, ?] =
    HttpClient(system, new URI(s"http://localhost:$port"), HttpMethod.Post)

  override def serverTransport: ServerTransport[Effect, Context] =
    NanoServer[Effect](system, port)
}

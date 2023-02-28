package test.transport.http

import automorph.Types
import automorph.spi.transport.ClientMessageTransport
import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.transport.http.HttpMethod
import automorph.transport.http.client.UrlClient
import automorph.transport.http.server.NanoServer
import java.net.URI
import org.scalacheck.Arbitrary
import test.core.ClientServerTest

class UrlClientHttpIdentityTest extends ClientServerTest {

  type Effect[T] = Identity[T]
  type Context = NanoServer.Context

  override lazy val system: IdentitySystem = IdentitySystem()

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def clientTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] = {
    val server = withRandomAvailablePort(port => NanoServer[Effect](handler, port))
    servers += server
    val url = new URI(s"http://localhost:${server.port}")
    val client = UrlClient(system, url, HttpMethod.Get).asInstanceOf[ClientMessageTransport[Effect, Context]]
    clients += client
    Some(client)
  }

  override def execute[T](effect: Effect[T]): T =
    effect
}

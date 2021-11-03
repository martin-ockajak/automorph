package test.transport.http

import automorph.Types
import automorph.spi.transport.ClientMessageTransport
import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.transport.http.HttpMethod
import automorph.transport.http.client.HttpClient
import automorph.transport.http.server.NanoServer
import java.net.URI
import org.scalacheck.Arbitrary
import test.core.ClientServerTest
import test.transport.http.HttpContextGenerator

class HttpClientHttpIdentityTest extends ClientServerTest {

  type Effect[T] = Identity[T]
  type Context = NanoServer.Context

  override lazy val system: IdentitySystem = IdentitySystem()
  override lazy val arbitraryContext: Arbitrary[Context] = HttpContextGenerator.arbitrary

  override def run[T](effect: Effect[T]): T = effect

  override def customTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] = {
    val server = withAvailablePort(port => NanoServer.create[Effect](handler, port)(run(_)))
    servers += server
    val url = new URI(s"http://localhost:${server.port}")
    val client = HttpClient(system, url, HttpMethod.Put).asInstanceOf[ClientMessageTransport[Effect, Context]]
    clients += client
    Some(client)
  }
}

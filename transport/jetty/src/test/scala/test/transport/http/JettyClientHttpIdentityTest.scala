package test.transport.http

import automorph.spi.ClientTransport
import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.transport.http.HttpMethod
import automorph.transport.http.client.JettyClient
import java.net.URI
import org.scalacheck.Arbitrary
import test.standard.StandardHttpClientTest

class JettyClientHttpIdentityTest extends StandardHttpClientTest {

  type Effect[T] = Identity[T]
  type Context = JettyClient.Context

  override lazy val system: IdentitySystem = IdentitySystem()

  override def run[T](effect: Effect[T]): T =
    effect

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def clientTransport(id: Int): ClientTransport[Effect, Context] = {
    System.setProperty("org.eclipse.jetty.LEVEL", "ERROR")
    JettyClient(system, new URI(s"http://localhost:${port(id)}"), HttpMethod.Post)
  }
}

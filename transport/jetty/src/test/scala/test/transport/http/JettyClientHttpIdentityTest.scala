package test.transport.http

import automorph.spi.transport.ClientMessageTransport
import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.transport.http.client.JettyClient
import java.net.URI
import automorph.transport.http.HttpMethod
import org.scalacheck.Arbitrary
import test.standard.StandardHttpClientTest
import test.transport.http.HttpContextGenerator

class JettyClientHttpIdentityTest extends StandardHttpClientTest {

  type Effect[T] = Identity[T]
  type Context = JettyClient.Context

  override lazy val system: IdentitySystem = IdentitySystem()

  override def execute[T](effect: Effect[T]): T =
    effect

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def clientTransport(url: URI): ClientMessageTransport[Effect, Context] = {
    System.setProperty("org.eclipse.jetty.LEVEL", "ERROR")
    JettyClient(system, url, HttpMethod.Put)
  }
}

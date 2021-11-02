package transport.http

import automorph.Types
import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.transport.http.client.SttpClient
import java.net.URI
import org.scalacheck.Arbitrary
import sttp.client3.HttpURLConnectionBackend
import sttp.model.Method
import test.standard.StandardHttpClientTest
import test.transport.http.HttpContextGenerator

class SttpClientHttpIdentityTest extends StandardHttpClientTest {

  type Effect[T] = Identity[T]
  type Context = SttpClient.Context

  override lazy val arbitraryContext: Arbitrary[Context] = HttpContextGenerator.arbitrary
  override lazy val system: EffectSystem[Effect] = IdentitySystem()

  def clientTransport(url: URI): ClientMessageTransport[Effect, Context] =
    SttpClient(url, Method.PUT.toString, HttpURLConnectionBackend(), system)

  override def run[T](effect: Effect[T]): T = effect

  override def runEffect[T](effect: Effect[T]): Unit = ()
}

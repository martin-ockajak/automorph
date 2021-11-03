package transport.http

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

  override lazy val system: IdentitySystem = IdentitySystem()
  
  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def clientTransport(url: URI): ClientMessageTransport[Effect, Context] =
    SttpClient.http(system, HttpURLConnectionBackend(), url, Method.PUT)

  override def run[T](effect: Effect[T]): T = effect
}

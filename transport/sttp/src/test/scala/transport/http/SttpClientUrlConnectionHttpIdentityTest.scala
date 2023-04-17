package transport.http

import automorph.spi.ClientTransport
import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.transport.http.HttpMethod
import automorph.transport.http.client.SttpClient
import org.scalacheck.Arbitrary
import sttp.client3.HttpURLConnectionBackend
import test.standard.StandardHttpClientTest
import test.transport.http.HttpContextGenerator

class SttpClientUrlConnectionHttpIdentityTest extends StandardHttpClientTest {

  type Effect[T] = Identity[T]
  type Context = SttpClient.Context

  override lazy val system: IdentitySystem = IdentitySystem()

  override def run[T](effect: Effect[T]): T =
    effect

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def clientTransport(id: Int): ClientTransport[Effect, ?] =
    SttpClient.http(system, HttpURLConnectionBackend(), url(id), HttpMethod.Post)

  override def portRange: Range =
    Range(20000, 25000)
}

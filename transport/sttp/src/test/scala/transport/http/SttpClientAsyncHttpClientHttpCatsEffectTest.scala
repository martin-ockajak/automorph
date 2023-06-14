package transport.http

import automorph.spi.{ClientTransport, EffectSystem}
import automorph.system.CatsEffectSystem
import automorph.transport.http.HttpMethod
import automorph.transport.http.client.SttpClient
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalacheck.Arbitrary
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import test.standard.StandardHttpClientTest
import test.transport.http.HttpContextGenerator

class SttpClientAsyncHttpClientHttpCatsEffectTest extends StandardHttpClientTest {

  type Effect[T] = IO[T]
  type Context = SttpClient.Context

  override lazy val system: EffectSystem[Effect] = CatsEffectSystem()

  override def run[T](effect: Effect[T]): T =
    effect.unsafeRunSync()

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def clientTransport(id: Int): ClientTransport[Effect, ?] =
    SttpClient.http(system, run(AsyncHttpClientCatsBackend()), url(id), HttpMethod.Post)

  override def integration: Boolean =
    true
}

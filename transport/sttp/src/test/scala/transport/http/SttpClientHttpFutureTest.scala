package transport.http

import automorph.Types
import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.system.FutureSystem
import automorph.transport.http.client.SttpClient
import java.net.URI
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.model.Method
import test.standard.StandardHttpClientTest
import test.transport.http.HttpContextGenerator

class SttpClientHttpFutureTest extends StandardHttpClientTest {

  type Effect[T] = Future[T]
  type Context = SttpClient.Context

  override lazy val arbitraryContext: Arbitrary[Context] = HttpContextGenerator.arbitrary
  override lazy val system: EffectSystem[Effect] = FutureSystem()

  def clientTransport(url: URI): ClientMessageTransport[Effect, Context] =
    SttpClient(url, Method.POST.toString, AsyncHttpClientFutureBackend(), system)

  override def run[T](effect: Effect[T]): T = await(effect)

  override def runEffect[T](effect: Effect[T]): Unit = ()
}

package transport.http

import automorph.spi.transport.ClientMessageTransport
import automorph.system.FutureSystem
import automorph.transport.http.client.SttpClient
import java.net.URI
import org.scalacheck.Arbitrary
import scala.concurrent.Future
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.model.Method
import test.standard.StandardHttpClientTest
import test.transport.http.HttpContextGenerator

class SttpClientHttpFutureTest extends StandardHttpClientTest {

  type Effect[T] = Future[T]
  type Context = SttpClient.Context

  override lazy val system: FutureSystem = FutureSystem()
  override lazy val arbitraryContext: Arbitrary[Context] = HttpContextGenerator.arbitrary

  override def clientTransport(url: URI): ClientMessageTransport[Effect, Context] =
    SttpClient.http(url, Method.POST.toString, AsyncHttpClientFutureBackend(), system)

  override def run[T](effect: Effect[T]): T = await(effect)
}

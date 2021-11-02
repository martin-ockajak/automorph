package transport.websocket

import automorph.spi.transport.ClientMessageTransport
import automorph.system.FutureSystem
import automorph.transport.http.client.SttpClient
import java.net.URI
import org.scalacheck.Arbitrary
import scala.concurrent.Future
import sttp.client3.httpclient.HttpClientFutureBackend
import sttp.model.Method
import test.standard.StandardHttpClientTest
import test.transport.http.HttpContextGenerator

class SttpClientWebSocketFutureTest extends StandardHttpClientTest {

  type Effect[T] = Future[T]
  type Context = SttpClient.Context

  override lazy val system: FutureSystem = FutureSystem()
  override lazy val arbitraryContext: Arbitrary[Context] = HttpContextGenerator.arbitrary

  override def webSocket: Boolean = true

  override def clientTransport(url: URI): ClientMessageTransport[Effect, Context] =
    SttpClient(url, Method.GET.toString, HttpClientFutureBackend(), system)

  override def run[T](effect: Effect[T]): T = await(effect)
}

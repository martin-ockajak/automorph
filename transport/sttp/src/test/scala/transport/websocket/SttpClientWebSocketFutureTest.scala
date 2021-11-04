package transport.websocket

import automorph.spi.transport.ClientMessageTransport
import automorph.system.FutureSystem
import automorph.transport.http.client.SttpClient
import java.net.URI
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import sttp.client3.httpclient.HttpClientFutureBackend
import automorph.transport.http.HttpMethod
import test.standard.StandardHttpClientTest
import test.transport.http.HttpContextGenerator

class SttpClientWebSocketFutureTest extends StandardHttpClientTest {

  type Effect[T] = Future[T]
  type Context = SttpClient.Context

  override lazy val system: FutureSystem = FutureSystem()

  override def execute[T](effect: Effect[T]): T =
    await(effect)

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def clientTransport(url: URI): ClientMessageTransport[Effect, Context] =
    SttpClient(system, HttpClientFutureBackend(), url, HttpMethod.Get)

  override def webSocket: Boolean = true
}

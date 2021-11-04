package test.transport.websocket

import automorph.spi.transport.ClientMessageTransport
import automorph.system.FutureSystem
import automorph.transport.http.client.JettyClient
import java.net.URI
import org.eclipse.jetty.http.HttpMethod
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.standard.StandardHttpClientTest
import test.transport.http.HttpContextGenerator

class JettyClientWebSocketFutureTest extends StandardHttpClientTest {

  type Effect[T] = Future[T]
  type Context = JettyClient.Context

  override lazy val system: FutureSystem = FutureSystem()

  override def execute[T](effect: Effect[T]): T =
    await(effect)

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def clientTransport(url: URI): ClientMessageTransport[Effect, Context] = {
    System.setProperty("org.eclipse.jetty.LEVEL", "ERROR")
    JettyClient(system, url, HttpMethod.GET)
  }

  override def webSocket: Boolean = true
}

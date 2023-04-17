package test.transport.websocket

import automorph.spi.{EffectSystem, EndpointTransport, ServerTransport}
import automorph.system.FutureSystem
import automorph.transport.http.server.UndertowServer
import automorph.transport.websocket.endpoint.UndertowWebSocketEndpoint
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.standard.StandardHttpServerTest
import test.transport.http.HttpContextGenerator

class UndertowServerWebSocketFutureTest extends StandardHttpServerTest {

  type Effect[T] = Future[T]
  type Context = UndertowServer.Context

  override lazy val system: EffectSystem[Effect] = FutureSystem()

  override def run[T](effect: Effect[T]): T =
    await(effect)

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def serverTransport(id: Int): ServerTransport[Effect, Context] =
    UndertowServer[Effect](system, port(id))

  override def endpointTransport: EndpointTransport[Future, Context, ?] =
    UndertowWebSocketEndpoint(system)

  override def webSocket: Boolean =
    true

  override def portRange: Range =
    Range(30000, 35000)
}

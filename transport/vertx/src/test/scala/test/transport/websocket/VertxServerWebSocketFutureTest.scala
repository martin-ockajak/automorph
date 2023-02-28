package test.transport.websocket

import automorph.Types
import automorph.spi.ServerTransport
import automorph.system.FutureSystem
import automorph.transport.http.server.VertxServer
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.standard.StandardHttpServerTest
import test.transport.http.HttpContextGenerator

class VertxServerWebSocketFutureTest extends StandardHttpServerTest {

  type Effect[T] = Future[T]
  type Context = VertxServer.Context

  override lazy val system: FutureSystem = FutureSystem()

  override def execute[T](effect: Effect[T]): T =
    await(effect)

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def serverTransport(
    handler: Types.HandlerAnyCodec[Effect, Context],
    port: Int,
  ): ServerTransport[Effect, Context] =
    VertxServer(handler, port)

  override def webSocket: Boolean =
    true
}

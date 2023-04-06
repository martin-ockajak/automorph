package test.transport.http

import automorph.spi.{EffectSystem, RequestHandler, ServerTransport}
import automorph.system.FutureSystem
import automorph.transport.http.endpoint.TapirHttpEndpoint
import java.net.InetSocketAddress
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import sttp.tapir.server.netty.{NettyFutureServer, NettyFutureServerBinding}
import test.standard.StandardHttpServerTest
import test.transport.http.TapirNettyHttpFutureTest.TapirServer

class TapirNettyHttpFutureTest extends StandardHttpServerTest {

  type Effect[T] = Future[T]
  type Context = TapirHttpEndpoint.Context

  override lazy val system: FutureSystem = FutureSystem()

  override def run[T](effect: Effect[T]): T =
    await(effect)

  override lazy val arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  def serverTransport(id: Int): ServerTransport[Effect, Context] =
    TapirServer(system, port(id))
}

case object TapirNettyHttpFutureTest {

  type Effect[T] = Future[T]
  type Context = TapirHttpEndpoint.Context

  final case class TapirServer(effectSystem: EffectSystem[Effect], port: Int) extends ServerTransport[Effect, Context] {
    private var endpoint = TapirHttpEndpoint(effectSystem)
    private var server = Option.empty[NettyFutureServerBinding[InetSocketAddress]]

    override def clone(handler: RequestHandler[Effect, Context]): ServerTransport[Effect, Context] = {
      endpoint = endpoint.clone(handler)
      this
    }

    override def init(): Effect[Unit] = {
      NettyFutureServer().addEndpoint(endpoint.adapter).start().map { activeServer =>
        server = Some(activeServer)
        ()
      }
    }

    override def close(): Effect[Unit] = {
      server.map { activeServer =>
        activeServer.stop()
      }.getOrElse(effectSystem.successful {})
    }
  }
}

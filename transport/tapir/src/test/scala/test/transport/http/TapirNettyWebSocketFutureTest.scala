//package test.transport.http
//
//import automorph.spi.{EffectSystem, RequestHandler, ServerTransport}
//import automorph.system.FutureSystem
//import automorph.transport.websocket.endpoint.TapirWebSocketEndpoint
//import java.net.InetSocketAddress
//import org.scalacheck.Arbitrary
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//import sttp.tapir.server.netty.{NettyFutureServer, NettyFutureServerBinding}
//import test.standard.StandardHttpServerTest
//import test.transport.http.TapirNettyWebSocketFutureTest.TapirServer
//
//class TapirNettyWebSocketFutureTest extends StandardHttpServerTest {
//
//  type Effect[T] = Future[T]
//  type Context = TapirWebSocketEndpoint.Context
//
//  override lazy val system: EffectSystem[Effect] = FutureSystem()
//
//  override def run[T](effect: Effect[T]): T =
//    await(effect)
//
//  override lazy val arbitraryContext: Arbitrary[Context] =
//    HttpContextGenerator.arbitrary
//
//  def serverTransport(id: Int): ServerTransport[Effect, Context] =
//    TapirServer(system, port(id))
//
//  override def webSocket: Boolean =
//    true
//
//  override def portRange: Range =
//    Range(25000, 30000)
//}
//
//case object TapirNettyWebSocketFutureTest {
//
//  type Effect[T] = Future[T]
//  type Context = TapirWebSocketEndpoint.Context
//
//  final case class TapirServer(effectSystem: EffectSystem[Effect], port: Int) extends ServerTransport[Effect, Context] {
//    private var endpoint = TapirWebSocketEndpoint(effectSystem)
//    private var server = Option.empty[NettyFutureServerBinding[InetSocketAddress]]
//
//    override def withHandler(handler: RequestHandler[Effect, Context]): ServerTransport[Effect, Context] = {
//      endpoint = endpoint.withHandler(handler)
//      this
//    }
//
//    override def init(): Effect[Unit] = {
//      NettyFutureServer().port(port).addEndpoint(endpoint.adapter).start().map { activeServer =>
//        server = Some(activeServer)
//      }
//    }
//
//    override def close(): Effect[Unit] = {
//      server.map { activeServer =>
//        activeServer.stop()
//      }.getOrElse(effectSystem.successful {})
//    }
//  }
//}

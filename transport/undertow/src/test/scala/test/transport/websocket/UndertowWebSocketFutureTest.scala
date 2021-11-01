//package test.transport.websocket
//
//import automorph.Types
//import automorph.spi.EffectSystem
//import automorph.spi.system.Defer
//import automorph.spi.transport.ServerMessageTransport
//import automorph.system.FutureSystem
//import automorph.transport.http.server.UndertowServer
//import org.scalacheck.Arbitrary
//import scala.collection.mutable.ArrayBuffer
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//import test.standard.StandardHttpServerTest
//import test.transport.http.HttpContextGenerator
//
//class UndertowWebSocketFutureTest extends StandardHttpServerTest {
//
//  type Effect[T] = Future[T]
//  type Context = UndertowServer.Context
//
//  override lazy val deferSystem: EffectSystem[Effect] with Defer[Effect] = FutureSystem()
//  override lazy val arbitraryContext: Arbitrary[Context] = HttpContextGenerator.arbitrary
//
//  def serverTransport(
//    handler: Types.HandlerAnyCodec[Effect, Context],
//    port: Int
//  ): ServerMessageTransport[Effect] =
//    UndertowServer.create(handler, port)(runEffect)
//
//  override def webSocket: Boolean = true
//
//  override def run[T](effect: Effect[T]): T = await(effect)
//
//  override def runEffect[T](effect: Effect[T]): Unit = ()
//}

//package test.transport.http
//
//import automorph.Types
//import automorph.spi.EffectSystem
//import automorph.spi.transport.ServerMessageTransport
//import automorph.system.FutureSystem
//import automorph.transport.http.server.UndertowServer
//import org.scalacheck.Arbitrary
//import scala.collection.mutable.ArrayBuffer
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.{Future, Promise}
//import test.standard.StandardHttpServerTest
//import test.transport.http.HttpContextGenerator
//
//class UndertowHttpFutureTest extends StandardHttpServerTest {
//
//  type Effect[T] = Future[T]
//  type Context = UndertowServer.Context
//
//  override lazy val deferSystem = FutureSystem()
//  override lazy val arbitraryContext: Arbitrary[Context] = HttpContextGenerator.arbitrary
//
//  def serverTransport(
//    handler: Types.HandlerAnyCodec[Effect, Context],
//    port: Int
//  ): (ServerMessageTransport[Effect], Int) =
//    UndertowServer.create(handler, port)(runEffect) -> port
//
//  override def run[T](effect: Effect[T]): T = await(effect)
//
//  override def runEffect[T](effect: Effect[T]): Unit = ()
//}

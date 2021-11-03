//package test.transport.http
//
//import automorph.Types
//import automorph.spi.transport.ServerMessageTransport
//import automorph.system.FutureSystem
//import automorph.transport.http.server.UndertowServer
//import org.scalacheck.Arbitrary
//import scala.collection.mutable.ArrayBuffer
//import scala.concurrent.Future
//import test.standard.StandardHttpServerTest
//import test.transport.http.HttpContextGenerator
//
//class UndertowHttpFutureTest extends StandardHttpServerTest {
//
//  type Effect[T] = Future[T]
//  type Context = UndertowServer.Context
//
//  override lazy val system: FutureSystem = FutureSystem()

//  override def arbitraryContext: Arbitrary[Context] =
//    HttpContextGenerator.arbitrary
//
//  override def serverTransport(
//    handler: Types.HandlerAnyCodec[Effect, Context],
//    port: Int
//  ): ServerMessageTransport[Effect] =
//    UndertowServer.create(handler, port)(runEffect)
//
//  override def run[T](effect: Effect[T]): T = await(effect)
//}

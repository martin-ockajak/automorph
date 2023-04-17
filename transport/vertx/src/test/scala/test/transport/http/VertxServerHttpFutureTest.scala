//package test.transport.http
//
//import automorph.spi.{EffectSystem, EndpointTransport, ServerTransport}
//import automorph.system.FutureSystem
//import automorph.transport.http.endpoint.VertxHttpEndpoint
//import automorph.transport.http.server.VertxServer
//import org.scalacheck.Arbitrary
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//import test.standard.StandardHttpServerTest
//
//class VertxServerHttpFutureTest extends StandardHttpServerTest {
//
//  type Effect[T] = Future[T]
//  type Context = VertxServer.Context
//
//  override lazy val system: EffectSystem[Effect] = FutureSystem()
//
//  override def run[T](effect: Effect[T]): T =
//    await(effect)
//
//  override def arbitraryContext: Arbitrary[Context] =
//    HttpContextGenerator.arbitrary
//
//  override def serverTransport(id: Int): ServerTransport[Effect, Context] =
//    VertxServer(system, port(id))
//
//  override def endpointTransport: EndpointTransport[Future, Context, ?] =
//    VertxHttpEndpoint(system)
//
//  override def testServerClose: Boolean =
//    false
//
//  override def portRange: Range =
//    Range(35000, 40000)
//}

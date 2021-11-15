//package test.transport.http
//
//import automorph.Types
//import automorph.spi.transport.ServerMessageTransport
//import automorph.system.FutureSystem
//import automorph.transport.http.endpoint.RapidoidHttpEndpoint
//import org.rapidoid.http.Req
//import org.rapidoid.setup.On
//import org.scalacheck.Arbitrary
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//import test.standard.StandardHttpServerTest
//import test.transport.http.HttpContextGenerator
//
//class RapidoidEndpointHttpFutureTest extends StandardHttpServerTest {
//
//  type Effect[T] = Future[T]
//  type Context = RapidoidHttpEndpoint.Context
//
//  override lazy val system: FutureSystem = FutureSystem()
//
//  override def execute[T](effect: Effect[T]): T =
//    await(effect)
//
//  override def arbitraryContext: Arbitrary[Context] =
//    HttpContextGenerator.arbitrary
//
//  override def serverTransport(
//    handler: Types.HandlerAnyCodec[Effect, Context],
//    port: Int
//  ): ServerMessageTransport[Effect] = new ServerMessageTransport[Effect] {
//    private val server = {
//      val endpoint = RapidoidHttpEndpoint(handler)
//      val setup = On.req(endpoint).port(port)
//      setup.activate()
//      setup
//    }
//
//    override def close(): Effect[Unit] =
//      Future(server.destroy())
//  }
//}

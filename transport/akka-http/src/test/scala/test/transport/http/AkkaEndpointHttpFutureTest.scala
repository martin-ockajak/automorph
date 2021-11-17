//package test.transport.http
//
//import automorph.Types
//import automorph.spi.transport.ServerMessageTransport
//import automorph.system.FutureSystem
//import automorph.transport.http.endpoint.RapidoidHttpEndpoint
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
//      val app = new App()
//      On.port(port)
//      On.req(endpoint).port(port)
//    }
//
//    override def close(): Effect[Unit] =
//      Future(server.destroy())
//  }
//}

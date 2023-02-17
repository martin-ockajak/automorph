//package test.transport.http
//
//import automorph.Types
//import automorph.spi.EffectSystem
//import automorph.spi.transport.ServerMessageTransport
//import automorph.system.FutureSystem
//import automorph.transport.http.HttpMethod
//import automorph.transport.http.endpoint.TapirHttpEndpoint
//import io.vertx.core.Vertx
//import io.vertx.ext.web.Router
//import org.scalacheck.Arbitrary
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//import sttp.model.Method
//import sttp.tapir.server.vertx.VertxFutureServerInterpreter
//import sttp.tapir.server.vertx.VertxFutureServerInterpreter.VertxFutureToScalaFuture
//import test.standard.StandardHttpServerTest
//import test.transport.http.HttpContextGenerator
//
//class TapirVertxHttpFutureTest extends StandardHttpServerTest {
//
//  type Effect[T] = Future[T]
//  type Context = TapirHttpEndpoint.Context
//
//  override lazy val system: FutureSystem = FutureSystem()
//
//  override def execute[T](effect: Effect[T]): T =
//    await(effect)
//
//  override lazy val arbitraryContext: Arbitrary[Context] =
//    HttpContextGenerator.arbitrary
//
//  def serverTransport(
//    handler: Types.HandlerAnyCodec[Effect, Context],
//    port: Int
//  ): ServerMessageTransport[Effect] = new ServerMessageTransport[Effect] {
//
//    private val server = {
//      val endpoint = TapirHttpEndpoint[Future](handler, Method.POST)
//      val vertx = Vertx.vertx()
//      val router = Router.router(vertx)
//      VertxFutureServerInterpreter().route(endpoint)(router)
//      val server = vertx.createHttpServer()
//      server.requestHandler(router).listen(port)
//      server
//    }
//
//    override def close(): Effect[Unit] =
//      server.close().asScala.map(_ => ())
//  }
//}

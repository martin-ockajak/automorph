//package test.transport.http
//
//import automorph.spi.{EffectSystem, RequestHandler, ServerTransport}
//import automorph.system.FutureSystem
//import automorph.transport.http.endpoint.TapirHttpEndpoint
//import io.vertx.core.Vertx
//import io.vertx.core.http.HttpServer
//import io.vertx.ext.web.Router
//import org.scalacheck.Arbitrary
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//import sttp.tapir.server.vertx.VertxFutureServerInterpreter
//import test.standard.StandardHttpServerTest
//import test.transport.http.TapirVertxHttpFutureTest.TapirServer
//
//class TapirVertxHttpFutureTest extends StandardHttpServerTest {
//
//  type Effect[T] = Future[T]
//  type Context = TapirHttpEndpoint.Context
//
//  override lazy val system: FutureSystem = FutureSystem()
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
//  override def portRange: Range =
//    Range(25000, 30000)
//}
//
//case object TapirVertxHttpFutureTest {
//
//  type Effect[T] = Future[T]
//  type Context = TapirHttpEndpoint.Context
//
//  final case class TapirServer(effectSystem: EffectSystem[Effect], port: Int) extends ServerTransport[Effect, Context] {
//    private var endpoint = TapirHttpEndpoint(effectSystem)
//    private var server = Option.empty[HttpServer]
//
//    override def withHandler(handler: RequestHandler[Effect, Context]): ServerTransport[Effect, Context] = {
//      endpoint = endpoint.withHandler(handler)
//      val vertx = Vertx.vertx()
//      val router = Router.router(vertx)
////      router.route(HttpMethod.POST, "/").handler { context =>
////        println(context.request().path())
////        context.response().write("TEST")
////        context.response().end()
////      }
//      VertxFutureServerInterpreter().route(endpoint.adapter)(router)
//      println(router.getRoutes.get(0).getPath)
//      println(router.getRoutes.get(0).methods())
//      server = Some(vertx.createHttpServer().requestHandler(router))
//      this
//    }
//
//    override def init(): Effect[Unit] =
//      effectSystem.evaluate {
//        server = server.map(_.listen(port).toCompletionStage.toCompletableFuture.get())
//      }
//
//    override def close(): Effect[Unit] = {
//      server.map { activeServer =>
//        effectSystem.evaluate {
//          activeServer.close().toCompletionStage.toCompletableFuture.get()
//          ()
//        }
//      }.getOrElse(effectSystem.successful {})
//    }
//  }
//}

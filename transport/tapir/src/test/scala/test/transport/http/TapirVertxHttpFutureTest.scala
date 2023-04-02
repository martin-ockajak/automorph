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
//import sttp.model.Method
//import sttp.tapir.server.vertx.VertxFutureServerInterpreter
//import sttp.tapir.server.vertx.VertxFutureServerInterpreter.VertxFutureToScalaFuture
//import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
//import sttp.tapir.server.finatra.FinatraServerInterpreter
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
//}
//
//case object TapirVertxHttpFutureTest {
//
//  type Effect[T] = Future[T]
//  type Context = TapirHttpEndpoint.Context
//
//  final case class TapirServer(
//    effectSystem: EffectSystem[Effect],
//    port: Int
//  ) extends ServerTransport[Effect, Context] {
//    private var endpoint = TapirHttpEndpoint(effectSystem)
//    private var server: HttpServer = None.orNull
//
//    override def clone(handler: RequestHandler[Effect, Context]): ServerTransport[Effect, Context] = {
//      endpoint = endpoint.clone(handler)
//      val tapirEndpoint = TapirHttpEndpoint[Future](effectSystem)
//      val vertx = Vertx.vertx()
//      val router = Router.router(vertx)
//      VertxFutureServerInterpreter().route(tapirEndpoint)(router)
//      server = vertx.createHttpServer().requestHandler(router)
//      this
//    }
//
//    override def init(): Effect[Unit] =
//      effectSystem.evaluate {
//        server = server.listen(port).toCompletionStage.toCompletableFuture.get()
//      }
//
//    override def close(): Effect[Unit] =
//      effectSystem.evaluate {
//        server.close().toCompletionStage.toCompletableFuture.get()
//      }
//  }
//}

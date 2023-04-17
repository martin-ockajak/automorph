//package test.transport.http
//
//import automorph.spi.{EffectSystem, RequestHandler, ServerTransport}
//import automorph.system.FutureSystem
//import automorph.transport.http.endpoint.TapirHttpEndpoint
//import com.twitter.finatra.http.routing.HttpRouter
//import com.twitter.finatra.http.{Controller, HttpServer}
//import org.scalacheck.Arbitrary
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//import sttp.tapir.server.finatra.{FinatraRoute, FinatraServerInterpreter, TapirController}
//import test.standard.StandardHttpServerTest
//import test.transport.http.TapirFinatraHttpFutureTest.TapirServer
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
//    FinatraServer(system, port(id))
//
//  override def portRange: Range =
//    Range(25000, 30000)
//}
//
//case object TapirFinatraHttpFutureTest {
//
//  type Effect[T] = Future[T]
//  type Context = TapirHttpEndpoint.Context
//
//  final case class FinatraController(route: FinatraRoute) extends Controller with TapirController {
//    addTapirRoute(route)
//  }
//
//  final case class FinatraHttpServer(port: Int) extends HttpServer {
//
//    override protected def defaultHttpPort: String =
//      s":$port"
//
//    override protected def configureHttp(router: HttpRouter): Unit =
//      router.add[FinatraController]
//  }
//
//  final case class TapirServer(effectSystem: EffectSystem[Effect], port: Int) extends ServerTransport[Effect, Context] {
//    private var endpoint = TapirHttpEndpoint(effectSystem)
//    private var server = Option.empty[HttpServer]
//
//    override def withHandler(handler: RequestHandler[Effect, Context]): ServerTransport[Effect, Context] = {
//      endpoint = endpoint.withHandler(handler)
//      val route = FinatraServerInterpreter().toRoute(endpoint.adapter)
//      server = FinatraHttpServer()
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
//
//        server.close().toCompletionStage.toCompletableFuture.get()
//      }
//  }
//}

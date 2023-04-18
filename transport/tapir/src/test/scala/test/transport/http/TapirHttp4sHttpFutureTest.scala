//package test.transport.http
//
//import automorph.spi.{EffectSystem, RequestHandler, ServerTransport}
//import automorph.system.CatsEffectSystem
//import automorph.transport.http.endpoint.TapirHttpEndpoint
//import cats.effect.IO
//import cats.effect.unsafe.implicits.global
//import java.net.InetSocketAddress
//import org.scalacheck.Arbitrary
//import sttp.tapir.server.http4s.Http4sServerInterpreter
//import test.standard.StandardHttpServerTest
//
//class TapirHttp4sHttpFutureTest extends StandardHttpServerTest {
//
//  type Effect[T] = IO[T]
//  type Context = TapirHttpEndpoint.Context
//
//  override lazy val system: EffectSystem[Effect] = CatsEffectSystem()
//
//  override def run[T](effect: Effect[T]): T =
//    effect.unsafeRunSync()
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
//case object TapirHttp4sHttpFutureTest {
//
//  type Effect[T] = IO[T]
//  type Context = TapirHttpEndpoint.Context
//
//  final case class TapirServer(effectSystem: EffectSystem[Effect], port: Int) extends ServerTransport[Effect, Context] {
//    private var endpoint = TapirHttpEndpoint(effectSystem)
//    private var server = Option.empty[Http4sFutureServerBinding[InetSocketAddress]]
//
//    override def withHandler(handler: RequestHandler[Effect, Context]): ServerTransport[Effect, Context] = {
//      endpoint = endpoint.withHandler(handler)
//      this
//    }
//
//    override def init(): Effect[Unit] = {
//      val routes = Http4sServerInterpreter[IO]().toRoutes(endpoint.adapter)
//      val service = routes.orNotFound.run(routes)
////        .map { activeServer =>
////        server = Some(activeServer)
////      }
//    }
//
//    override def close(): Effect[Unit] = {
//      server.map { activeServer =>
//        activeServer.stop()
//      }.getOrElse(effectSystem.successful {})
//    }
//  }
//}

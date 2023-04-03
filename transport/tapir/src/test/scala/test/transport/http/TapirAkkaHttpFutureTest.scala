//package test.transport.http
//
//import akka.actor.typed.ActorSystem
//import akka.actor.typed.scaladsl.Behaviors
//import akka.http.scaladsl.Http
//import automorph.spi.{EffectSystem, RequestHandler, ServerTransport}
//import automorph.system.FutureSystem
//import automorph.transport.http.endpoint.TapirHttpEndpoint
//import org.scalacheck.Arbitrary
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
//import test.standard.StandardHttpServerTest
//import test.transport.http.TapirAkkaHttpFutureTest.TapirServer
//
//class TapirAkkaHttpFutureTest extends StandardHttpServerTest {
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
//}
//
//case object TapirAkkaHttpFutureTest {
//
//  type Effect[T] = Future[T]
//  type Context = TapirHttpEndpoint.Context
//
//  final case class TapirServer(effectSystem: EffectSystem[Effect], port: Int) extends ServerTransport[Effect, Context] {
//    private var endpoint = TapirHttpEndpoint(effectSystem)
//    private var server = Option.empty[(ActorSystem[Nothing], Http.ServerBinding)]
//
//    override def clone(handler: RequestHandler[Effect, Context]): ServerTransport[Effect, Context] = {
//      endpoint = endpoint.clone(handler)
//      this
//    }
//
//    override def init(): Effect[Unit] = {
//      implicit val actorSystem: ActorSystem[Any] = ActorSystem(Behaviors.empty[Any], getClass.getSimpleName)
//      val route = AkkaHttpServerInterpreter().toRoute(endpoint.adapter)
//      Http().newServerAt("0.0.0.0", port).bind(route).map { serverBinding =>
//        server = Some((actorSystem, serverBinding))
//      }
//    }
//
//    override def close(): Effect[Unit] = {
//      server.map { case (actorSystem, serverBinding) =>
//        serverBinding.unbind().map(_ => actorSystem.terminate()).flatMap(_ => actorSystem.whenTerminated).map(_ => ())
//      }.getOrElse(effectSystem.successful {})
//    }
//  }
//}

package test.transport.http

import automorph.spi.{EffectSystem, RequestHandler, ServerTransport}
import automorph.system.CatsEffectSystem
import automorph.transport.http.endpoint.TapirHttpEndpoint
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.effect._
import cats.implicits._
import com.comcast.ip4s.Port
import org.http4s.ember.server.EmberServerBuilder
import org.scalacheck.Arbitrary
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import sttp.tapir.server.http4s.Http4sServerInterpreter
import test.standard.StandardHttpServerTest
import test.transport.http.TapirHttp4sHttpFutureTest.TapirServer

class TapirHttp4sHttpFutureTest extends StandardHttpServerTest {

  type Effect[T] = IO[T]
  type Context = TapirHttpEndpoint.Context

  override lazy val system: EffectSystem[Effect] = CatsEffectSystem()

  override def run[T](effect: Effect[T]): T =
    effect.unsafeRunSync()

  override lazy val arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  def serverTransport(id: Int): ServerTransport[Effect, Context] =
    TapirServer(system, port(id))

  override def portRange: Range =
    Range(25000, 30000)
}

case object TapirHttp4sHttpFutureTest {

  type Effect[T] = IO[T]
  type Context = TapirHttpEndpoint.Context

  final case class TapirServer(effectSystem: EffectSystem[Effect], port: Int) extends ServerTransport[Effect, Context] {
    private var endpoint = TapirHttpEndpoint(effectSystem)
    private var server = Option.empty[() => Future[Unit]]

    override def withHandler(handler: RequestHandler[Effect, Context]): ServerTransport[Effect, Context] = {
      endpoint = endpoint.withHandler(handler)
      this
    }

    override def init(): Effect[Unit] =
      effectSystem.evaluate {
        val service = Http4sServerInterpreter[IO]().toRoutes(endpoint.adapter).orNotFound
        val builder = EmberServerBuilder.default[IO].withPort(Port.fromInt(port).get).withHttpApp(service)
        server = Some(builder.build.useForever.unsafeRunCancelable())
      }

    override def close(): Effect[Unit] =
      server.map { activeServer =>
        effectSystem.evaluate {
          Await.result(activeServer(), Duration.Inf)
        }
      }.getOrElse(effectSystem.successful {})
    }
}

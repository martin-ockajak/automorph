package test.transport.http

import automorph.spi.{EffectSystem, RequestHandler, ServerTransport}
import automorph.system.FutureSystem
import automorph.transport.http.endpoint.FinagleHttpEndpoint
import com.twitter.finagle.{Http, ListeningServer}
import com.twitter.util.{Return, Throw}
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import test.standard.StandardHttpServerTest

class FinagleEndpointHttpFutureTest extends StandardHttpServerTest {

  type Effect[T] = Future[T]
  type Context = FinagleHttpEndpoint.Context

  override lazy val system: FutureSystem = FutureSystem()

  override def run[T](effect: Effect[T]): T =
    await(effect)

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def serverTransport: ServerTransport[Effect, Context] =
    FinagleServer(system)

  private final case class FinagleServer(effectSystem: EffectSystem[Effect]) extends ServerTransport[Effect, Context] {
    private var endpoint = FinagleHttpEndpoint(effectSystem)
    private var server: ListeningServer = None.orNull

    override def clone(handler: RequestHandler[Effect, Context]): ServerTransport[Effect, Context] = {
      endpoint = endpoint.clone(handler)
      this
    }

    override def init(): Effect[Unit] =
      Future {
        server = Http.serve(s":$port", endpoint)
        ()
      }

    override def close(): Effect[Unit] = {
      val promise = Promise[Unit]()
      server.close().respond {
        case Return(result) => promise.success(result)
        case Throw(error) => promise.failure(error)
      }
      promise.future
    }
  }
}

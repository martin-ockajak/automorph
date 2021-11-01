package test.transport.http

import automorph.Types
import automorph.spi.EffectSystem
import automorph.spi.system.Defer
import automorph.spi.transport.ServerMessageTransport
import automorph.system.FutureSystem
import automorph.transport.http.endpoint.FinagleEndpoint
import com.twitter.finagle.Http
import com.twitter.util.{Return, Throw}
import org.scalacheck.Arbitrary
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import test.standard.StandardHttpServerTest
import test.transport.http.HttpContextGenerator

class FinagleHttpFutureTest extends StandardHttpServerTest {

  type Effect[T] = Future[T]
  type Context = FinagleEndpoint.Context

  override lazy val deferSystem: EffectSystem[Effect] with Defer[Effect] = FutureSystem()
  override lazy val arbitraryContext: Arbitrary[Context] = HttpContextGenerator.arbitrary

  def serverTransport(
    handler: Types.HandlerAnyCodec[Effect, Context],
    port: Int
  ): ServerMessageTransport[Effect] = new ServerMessageTransport[Effect] {
    private val server = {
      val endpoint = FinagleEndpoint.create(handler)(runEffect)
      Http.serve(s":$port", endpoint)
    }

    override def close(): Future[Unit] = {
      val promise = Promise[Unit]
      server.close().respond {
        case Return(result) => promise.success(result)
        case Throw(error) => promise.failure(error)
      }
      promise.future
    }
  }

  override def run[T](effect: Effect[T]): T = await(effect)

  override def runEffect[T](effect: Effect[T]): Unit = ()
}

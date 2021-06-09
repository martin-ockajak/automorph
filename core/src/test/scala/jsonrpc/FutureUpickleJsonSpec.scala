package jsonrpc

import jsonrpc.FutureUpickleJsonSpec.Effect
import jsonrpc.UpickleJsonSpec.{CodecType, Node}
import jsonrpc.backend.standard.FutureBackend
import jsonrpc.client.UnnamedBinding
import jsonrpc.spi.Backend
import jsonrpc.transport.local.HandlerTransport
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FutureUpickleJsonSpec extends UpickleJsonSpec[Effect]:

  override def backend: Backend[Effect] = FutureBackend()

  override def run[T](effect: Effect[T]): T = await(effect)

  lazy val handler = Handler[Node, CodecType, Effect, Short](codec, backend)
    .bind(simpleApiInstance).bind[ComplexApi[Effect]](complexApiInstance)

  lazy val theLocalClient = Client(codec, backend, HandlerTransport(handler, backend, 0))

  override def localClient: Client[Node, CodecType, Effect, Short, UnnamedBinding[Node, CodecType, Effect, Short]] = theLocalClient

  override def simpleApis: TestedApis[SimpleApi[Effect]] = TestedApis(
    theLocalClient.bind,
    theLocalClient.positional.bind,
    theLocalClient.bind,
    theLocalClient.positional.bind,
  )

  override def complexApis: TestedApis[ComplexApi[Effect]] = TestedApis(
    theLocalClient.bind,
    theLocalClient.positional.bind,
    theLocalClient.bind,
    theLocalClient.positional.bind,
  )

object FutureUpickleJsonSpec:
  type Effect[T] = Future[T]

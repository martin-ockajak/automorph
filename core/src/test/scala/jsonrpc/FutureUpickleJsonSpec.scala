package jsonrpc

import jsonrpc.FutureUpickleJsonSpec.Effect
import jsonrpc.UpickleJsonSpec.{CodecType, Node}
import jsonrpc.backend.standard.FutureBackend
import jsonrpc.client.UnnamedBinding
import jsonrpc.spi.Backend
import jsonrpc.transport.local.HandlerTransport
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EffectUpickleJsonSpec extends UpickleJsonSpec[Effect]:

  override def backend: Backend[Effect] = FutureBackend()

  override def run[T](effect: Effect[T]): T = await(effect)

  lazy val handler = Handler[Node, CodecType, Effect, Short](codec, backend)
    .bind(simpleApiInstance).bind[ComplexApi[Effect]](complexApiInstance)

  lazy val theClient = Client(codec, backend, HandlerTransport(handler, backend, 0))

  override def client: Client[Node, CodecType, Effect, Short, UnnamedBinding[Node, CodecType, Effect, Short]] = theClient

  override def simpleApiLocal: SimpleApi[Effect] = theClient.bind[SimpleApi[Effect]]

  override def complexApiLocal: ComplexApi[Effect] = theClient.bind[ComplexApi[Effect]]

  override def simpleApiRemote: SimpleApi[Effect] = theClient.bind[SimpleApi[Effect]]

  override def complexApiRemote: ComplexApi[Effect] = theClient.bind[ComplexApi[Effect]]

object FutureUpickleJsonSpec:
  type Effect[T] = Future[T]

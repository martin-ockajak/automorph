package jsonrpc

import jsonrpc.FutureUpickleJsonSpec.Effect
import jsonrpc.UpickleJsonSpec.{CodecType, Node}
import jsonrpc.backend.standard.FutureBackend
import jsonrpc.spi.Backend
import jsonrpc.transport.local.HandlerTransport
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EffectUpickleJsonSpec extends UpickleJsonSpec[Effect]:

  override def backend: Backend[Effect] = FutureBackend()

  lazy val handler = Handler[Node, CodecType, Effect, Short](codec, backend)
    .bind(simpleApi).bind[ComplexApi[Effect]](complexApi)

  lazy val theClient = Client(codec, backend, HandlerTransport(handler, backend, 0))

  override def client: Client[Node, CodecType, Effect, Short] = theClient

  override def simpleApiProxy: SimpleApi[Effect] = theClient.bindByPosition[SimpleApi[Effect]]

  override def complexApiProxy: ComplexApi[Effect] = theClient.bindByName[ComplexApi[Effect]]

object FutureUpickleJsonSpec:
  type Effect[T] = Future[T]

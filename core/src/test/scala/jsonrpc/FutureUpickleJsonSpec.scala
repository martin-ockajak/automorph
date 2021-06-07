package jsonrpc

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import jsonrpc.client.{Client, ClientFactory}
import jsonrpc.codec.json.upickle.UpickleJsonCodec
import jsonrpc.ReadWriters
import jsonrpc.handler.{Handler, HandlerFactory}
import jsonrpc.spi.{Backend, Codec, Transport}
import jsonrpc.transport.local.HandlerTransport
import jsonrpc.FutureUpickleJsonSpec.Effect
import UpickleJsonSpec.Node
import UpickleJsonSpec.CodecType
import jsonrpc.spi.Backend
import jsonrpc.backend.standard.FutureBackend
import jsonrpc.handler.Handler

class EffectUpickleJsonSpec extends CoreSpec[Node, CodecType, Effect]:
  override def backend: Backend[Effect] = FutureBackend()

  val theClient =
    val codec = UpickleJsonCodec(ReadWriters)
//    val handler = HandlerFactory[Node, CodecType, Effect, Short](codec, backend).bind(simpleApi).bind[ComplexApi[Effect]](complexApi)
    val handler = HandlerFactory[Node, CodecType, Effect, Short](codec, backend).bind[ComplexApi[Effect]](complexApi)
    val transport = HandlerTransport[Node, CodecType, Effect, Short](handler, backend)
    ClientFactory[Node, CodecType, Effect, Short](codec, backend, transport)

  override def client: Client[Node, CodecType, Effect, Short] = theClient

  override def simpleApiProxy: SimpleApi[Effect] = theClient.bind[SimpleApi[Effect]]

  override def complexApiProxy: ComplexApi[Effect] = theClient.bind[ComplexApi[Effect]]

object FutureUpickleJsonSpec:
  type Effect[T] = Future[T]

//  def handlerx: Handler[Node, CodecType, Future, Short] = handler

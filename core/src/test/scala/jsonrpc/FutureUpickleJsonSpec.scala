package jsonrpc

import jsonrpc.FutureUpickleJsonSpec.Effect
import jsonrpc.ReadWriters
import jsonrpc.UpickleJsonSpec.{CodecType, Node}
import jsonrpc.backend.standard.FutureBackend
import jsonrpc.client.{Client, ClientFactory}
import jsonrpc.codec.json.upickle.UpickleJsonCodec
import jsonrpc.handler.HandlerFactory
import jsonrpc.spi.{Backend, Codec, Transport}
import jsonrpc.transport.local.HandlerTransport
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EffectUpickleJsonSpec extends CoreSpec[Node, CodecType, Effect]:
  override def backend: Backend[Effect] = FutureBackend()

  val theClient =
    val codec = UpickleJsonCodec(ReadWriters)
    val handler = HandlerFactory[Node, CodecType, Effect, Short](codec, backend).bind(simpleApi).bind[ComplexApi[Effect]](complexApi)
    val transport = HandlerTransport[Node, CodecType, Effect, Short](handler, backend)
    ClientFactory[Node, CodecType, Effect, Short](codec, backend, transport)

  override def client: Client[Node, CodecType, Effect, Short] = theClient

  override def simpleApiProxy: SimpleApi[Effect] = theClient.bind[SimpleApi[Effect]]

  override def complexApiProxy: ComplexApi[Effect] = theClient.bind[ComplexApi[Effect]]

object FutureUpickleJsonSpec:
  type Effect[T] = Future[T]

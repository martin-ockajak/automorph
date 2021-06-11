package jsonrpc.backend.standard

import jsonrpc.backend.standard.FutureUpickleJsonSpec.Effect
import jsonrpc.codec.json.UpickleJsonSpec.{CodecType, Node}
import jsonrpc.backend.standard.FutureBackend
import jsonrpc.client.UnnamedBinding
import jsonrpc.codec.json.UpickleJsonSpec
import jsonrpc.spi.Backend
import jsonrpc.transport.local.HandlerTransport
import jsonrpc.{Client, ComplexApi, Handler, InvalidApi, SimpleApi}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FutureUpickleJsonSpec extends UpickleJsonSpec[Effect]:

  override def backend: Backend[Effect] = FutureBackend()

  override def run[T](effect: Effect[T]): T = await(effect)

  override def client: Client[Node, CodecType, Effect, Short, UnnamedBinding[Node, CodecType, Effect, Short]] =
    Client(codec, backend, handlerTransport)

  override def simpleApis: Proxies[SimpleApi[Effect]] = Proxies(
    client.bind,
    client.positional.bind
  )

  override def complexApis: Proxies[ComplexApi[Effect]] = Proxies(
    client.bind,
    client.positional.bind
  )

  override def invalidApis: Proxies[InvalidApi[Effect]] = Proxies(
    client.bind,
    client.positional.bind
  )

object FutureUpickleJsonSpec:
  type Effect[T] = Future[T]

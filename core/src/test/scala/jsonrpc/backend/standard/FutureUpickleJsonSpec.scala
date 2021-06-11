package jsonrpc.backend.standard

import jsonrpc.backend.standard.FutureBackend
import jsonrpc.codec.json.UpickleJsonSpec
import jsonrpc.spi.Backend
import jsonrpc.transport.local.HandlerTransport
import jsonrpc.{Client, ComplexApi, InvalidApi, SimpleApi}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FutureUpickleJsonSpec extends UpickleJsonSpec:
  type Effect[T] = Future[T]

  override def backend: Backend[Effect] = FutureBackend()

  override def run[T](effect: Effect[T]): T = await(effect)

  override def client: Client[Node, CodecType, Effect, Short] =
    Client(codec, backend, handlerTransport)

  override def simpleApis: Seq[SimpleApi[Effect]] = clients.map(_.bind[SimpleApi[Effect]])

  override def complexApis: Seq[ComplexApi[Effect]] = clients.map(_.bind[ComplexApi[Effect]])

  override def invalidApis: Seq[InvalidApi[Effect]] = clients.map(_.bind[InvalidApi[Effect]])

  "" - {
    "test" in {
      client.callByPosition[String, String]("test")("test")(using 0)
    }
  }

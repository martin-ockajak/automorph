package jsonrpc

import base.BaseSpec
import io.circe.generic.auto.*
import jsonrpc.backend.monix.MonixBackend
import jsonrpc.backend.standard.{FutureBackend, NoBackend}
import jsonrpc.backend.zio.ZioBackend
import jsonrpc.codec.json.dummy.DummyJsonCodec
import jsonrpc.codec.json.upickle.UpickleJsonCodec
import jsonrpc.codec.json.circe.CirceJsonCodec
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.transport.local.HandlerTransport
import jsonrpc.{ApiImpl, Enum, JsonRpcHandler, Record, SimpleApi, Structure}
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import upickle.AttributeTagged
import zio.{FiberFailure, RIO, Runtime, ZEnv}

class HandlerSpec extends BaseSpec:
  private val simpleApi = SimpleApi()
  private val upickleCodec = UpickleJsonCodec(JsonPickler)
  private val circeCodec = CirceJsonCodec()
  private val noBackend = NoBackend()
  private val futureBackend = FutureBackend()
  private val monixBackend = MonixBackend()
  private val zioBackend = ZioBackend[ZEnv]()

  "" - {
    "Bind" - {
      "No context" in {
        JsonRpcHandler(DummyJsonCodec(), noBackend).bind(simpleApi)
      }
      "Upickle / No effect" in {
        testBind(upickleCodec, noBackend)
      }
      "Upickle / Future" in {
        testBind(upickleCodec, futureBackend)
      }
      "Upickle / Monix" in {
        testBind(upickleCodec, monixBackend)
      }
      "Upickle / Zio" in {
        testBind(upickleCodec, zioBackend)
      }
//      "Circe / Future" in {
//        testBind(circeCodec, futureBackend)
//      }
    }

  }

  private inline def testBind[Node, CodecType <: Codec[Node], Effect[_]](codec: CodecType, backend: Backend[Effect]): Unit =
    val api = ApiImpl(backend)
    val handler = JsonRpcHandler[Node, CodecType, Effect, Short](codec, backend).bind[Api[Effect]](api)
    val transport = HandlerTransport(handler, backend)
    val client = JsonRpcClient[Node, CodecType, Effect, Short](codec, backend, transport)
    val apiProxy = client.bind[Api[Effect]]

object JsonPickler extends AttributeTagged:

  given ReadWriter[Enum] = readwriter[Int].bimap[Enum](
    value => value.ordinal,
    number => Enum.fromOrdinal(number)
  )
  given ReadWriter[Structure] = macroRW
  given ReadWriter[Record] = macroRW

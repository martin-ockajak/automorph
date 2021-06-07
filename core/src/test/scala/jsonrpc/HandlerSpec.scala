package jsonrpc

import base.BaseSpec
import io.circe.generic.auto.*
//import jsonrpc.backend.cats.CatsBackend
import jsonrpc.backend.monix.MonixBackend
import jsonrpc.backend.scalaz.ScalazBackend
import jsonrpc.client.ClientFactory
import jsonrpc.handler.HandlerFactory
import jsonrpc.backend.standard.{FutureBackend, NoBackend, TryBackend}
import jsonrpc.backend.zio.ZioBackend
import jsonrpc.codec.json.dummy.DummyJsonCodec
import jsonrpc.codec.json.upickle.UpickleJsonCodec
import jsonrpc.codec.json.circe.CirceJsonCodec
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.transport.local.HandlerTransport
import jsonrpc.{ComplexApiImpl, Enum, Record, SimpleApi, Structure}
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import upickle.AttributeTagged
import zio.{FiberFailure, RIO, Runtime, ZEnv}

class HandlerSpec extends BaseSpec:
  private val upickleCodec = UpickleJsonCodec(JsonPickler)
  private val circeCodec = CirceJsonCodec()
  private val noBackend = NoBackend()
  private val tryBackend = TryBackend()
  private val futureBackend = FutureBackend()
  private val monixBackend = MonixBackend()
  private val zioBackend = ZioBackend[ZEnv]()
//  private val catsBackend = CatsBackend()
  private val scalazBackend = ScalazBackend()

  "" - {
    "Bind" - {
      "Dummy" - {
        "No context" in {
          val simpleApi = SimpleApi(noBackend)
          HandlerFactory(DummyJsonCodec(), noBackend).bind(simpleApi)
        }
      }
      "Upickle" - {
        val codec = upickleCodec
        "No effect" in {
          testBind(codec, noBackend)
        }
        "Try" in {
          testBind(codec, tryBackend)
        }
        "Future" in {
          testBind(codec, futureBackend)
        }
        "Monix" in {
          testBind(codec, monixBackend)
        }
//        "Zio" in {
//          testBind(codec, zioBackend)
//        }
//        "Cats" in {
//          testBind(codec, catsBackend)
//        }
        "Scalaz" in {
          testBind(codec, scalazBackend)
        }
      }
      "Circe" - {
        val codec = circeCodec
//        " Future" in {
//          testBind(codec, futureBackend)
//        }
      }
    }

  }

  private inline def testBind[Node, CodecType <: Codec[Node], Effect[_]](codec: CodecType, backend: Backend[Effect]): Unit =
    val api = ComplexApiImpl(backend)
    val handler = HandlerFactory[Node, CodecType, Effect, Short](codec, backend).bind[ComplexApi[Effect]](api)
    val transport = HandlerTransport(handler, backend)
    val client = ClientFactory[Node, CodecType, Effect, Short](codec, backend, transport)
    val apiProxy = client.bind[ComplexApi[Effect]]

object JsonPickler extends AttributeTagged:

  given ReadWriter[Enum] = readwriter[Int].bimap[Enum](
    value => value.ordinal,
    number => Enum.fromOrdinal(number)
  )
  given ReadWriter[Structure] = macroRW
  given ReadWriter[Record] = macroRW

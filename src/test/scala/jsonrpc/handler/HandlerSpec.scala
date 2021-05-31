package jsonrpc.handler

import base.BaseSpec
import jsonrpc.{ApiImpl, JsonRpcHandler, SimpleApi}
import jsonrpc.codec.json.dummy.DummyJsonCodec
import jsonrpc.codec.json.upickle.UpickleJsonCodec
import jsonrpc.effect.standard.FutureEffect
import jsonrpc.effect.standard.NoEffect
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HandlerSpec extends BaseSpec:

  "" - {
    "Bind" - {
      "Default" in {
        val api = ApiImpl("")
        val simpleApi = SimpleApi()
        JsonRpcHandler[String, DummyJsonCodec, NoEffect.Identity, String](DummyJsonCodec(), NoEffect()).bind(simpleApi)
        val handler = JsonRpcHandler[String, DummyJsonCodec, Future, String](DummyJsonCodec(), FutureEffect()).bind(api)
//        val handler = JsonRpcHandler[String, UpickleJsonCodec, Future, String](UpickleJsonCodec(), FutureEffect()).bind(api)
        (0 == 0).shouldBe(true)


      }
    }
  }

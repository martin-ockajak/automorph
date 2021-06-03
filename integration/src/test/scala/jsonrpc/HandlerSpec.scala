package jsonrpc

import base.BaseSpec
import jsonrpc.JsonRpcHandler.NoContext
import jsonrpc.codec.json.dummy.DummyJsonCodec
import jsonrpc.codec.json.upickle.UpickleJsonCodec
import jsonrpc.effect.standard.{FutureEffect, NoEffect}
import jsonrpc.{ApiImpl, JsonRpcHandler, SimpleApi}
import scala.collection.immutable.ArraySeq
import scala.concurrent.ExecutionContext.Implicits.global

class HandlerSpec extends BaseSpec:

  "" - {
    "Bind" - {
      "Default" in {
        val api = ApiImpl("")
        val simpleApi = SimpleApi()
        JsonRpcHandler[String, DummyJsonCodec, NoEffect.Identity, String](DummyJsonCodec(), NoEffect()).bind(simpleApi)
        JsonRpcHandler[String, DummyJsonCodec, NoEffect.Identity, NoContext](DummyJsonCodec(), NoEffect())
        val handler = JsonRpcHandler.basic(DummyJsonCodec(), FutureEffect()).bind(api)
//        handler.processRequest(ArraySeq.ofByte(Array.empty[Byte]))
//        val handler = JsonRpcHandler(UpickleJsonCodec(), FutureEffect()).bind(api)
        (0 == 0).shouldBe(true)

      }
    }
  }

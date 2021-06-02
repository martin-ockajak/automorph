package jsonrpc.handler

import base.BaseSpec
import jsonrpc.{ApiImpl, JsonRpcHandler, SimpleApi}
import jsonrpc.JsonRpcHandler.given
import jsonrpc.JsonRpcHandler.NoContext
import jsonrpc.codec.json.dummy.DummyJsonCodec
import jsonrpc.codec.json.upickle.UpickleJsonCodec
import jsonrpc.effect.standard.FutureEffect
import jsonrpc.effect.standard.NoEffect
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.collection.immutable.ArraySeq

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
//        handler.processRequest(ArraySeq.ofByte("".getBytes), ())
//        val handler = JsonRpcHandler(UpickleJsonCodec(), FutureEffect()).bind(api)
        (0 == 0).shouldBe(true)

      }
    }
  }

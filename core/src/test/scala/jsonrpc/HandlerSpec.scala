package jsonrpc

import base.BaseSpec
import jsonrpc.JsonRpcHandler.NoContext
import jsonrpc.codec.json.dummy.DummyJsonCodec
import jsonrpc.codec.json.upickle.UpickleJsonCodec
import jsonrpc.effect.standard.{FutureEffect, NoEffect}
import jsonrpc.transport.local.HandlerTransport
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
        val handler = JsonRpcHandler[String, DummyJsonCodec, NoEffect.Identity, NoContext](DummyJsonCodec(), NoEffect())
        val futureHandler = JsonRpcHandler.basic(DummyJsonCodec(), FutureEffect()).bind(api)
//        val futureHandler = JsonRpcHandler(UpickleJsonCodec(), FutureEffect()).bind(api)
//        handler.processRequest(ArraySeq.ofByte(Array.empty[Byte]))

        val transport = HandlerTransport(handler, NoEffect())
        val client = JsonRpcClient[String, DummyJsonCodec, NoEffect.Identity, NoContext](DummyJsonCodec(), NoEffect(), transport)
        client.bind[Api]
        (0 == 0).shouldBe(true)



      }
    }
  }

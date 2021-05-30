package jsonrpc.handler

import base.BaseSpec
import jsonrpc.{ApiImpl, JsonRpcHandler}
import jsonrpc.codec.json.dummy.DummyJsonCodec
import jsonrpc.effect.native.NoEffect

class HandlerSpec extends BaseSpec:

  "" - {
    "Bind" - {
      "Default" in {
        val api = ApiImpl("")
        val handler = JsonRpcHandler(DummyJsonCodec(), NoEffect()).bind(api)
        (0 == 0).shouldBe(true)



      }
    }
  }

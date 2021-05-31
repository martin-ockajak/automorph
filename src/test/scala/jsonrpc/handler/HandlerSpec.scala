package jsonrpc.handler

import base.BaseSpec
import jsonrpc.{ApiImpl, JsonRpcHandler}
import jsonrpc.codec.json.dummy.DummyJsonCodec
import jsonrpc.effect.standard.FutureEffect
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HandlerSpec extends BaseSpec:

  "" - {
    "Bind" - {
      "Default" in {
        val api = ApiImpl("")
        val handler = JsonRpcHandler[String, DummyJsonCodec, Future, String](DummyJsonCodec(), FutureEffect()).bind(api)
        (0 == 0).shouldBe(true)

      }
    }
  }

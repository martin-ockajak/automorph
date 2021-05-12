package jsonrpc

import base.BaseSpec
import jsonrpc.effect.PlainEffectContext
import jsonrpc.json.DummyJsonContext
import jsonrpc.server.ServerMacros
import scala.concurrent.Future
import scala.quoted.Quotes

class ServerSpec
  extends BaseSpec:

  "" - {
    "Bind" - {
      "Default" in {
        val server = JsonRpcServer(DummyJsonContext(), PlainEffectContext())
        val api = ApiImpl()
        server.bind[Api](api)
        (0 == 0).shouldBe(true)
      }
    }
  }

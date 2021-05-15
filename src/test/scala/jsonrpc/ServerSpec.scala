package jsonrpc

import base.BaseSpec
import jsonrpc.effect.native.PlainEffectContext
import jsonrpc.json.dummy.DummyJsonContext
import jsonrpc.server.ServerMacros
import scala.concurrent.Future
import scala.quoted.Quotes

class ServerSpec
  extends BaseSpec:

  "" - {
    "Bind" - {
      "Default" in {
        val server = JsonRpcServer(DummyJsonContext(), PlainEffectContext())
        val api = ApiImpl("")
        server.bind(api)
        (0 == 0).shouldBe(true)
      }
    }
  }

package jsonrpc

import jsonrpc.BaseSpec
import jsonrpc.effect.PlainEffectContext
import jsonrpc.json.DummyJsonContext

class TestSpec extends BaseSpec:
  "" - {
    "Category" - {
      "Test" in {
        val x = new CommonJsonRpcServer(DummyJsonContext(), PlainEffectContext())
        (0 == 0).shouldBe(true)
      }
    }
  }

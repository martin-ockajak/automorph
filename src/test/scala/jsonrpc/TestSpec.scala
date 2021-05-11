package jsonrpc

import jsonrpc.BaseSpec

class TestSpec extends BaseSpec:
  "" - {
    "Category" - {
      "Test" in {
        val x = new CommonJsonRpcServer(DummyJsonContext(), PlainEffectContext())
        (0 == 0).shouldBe(true)
      }
    }
  }

package test

import base.BaseSpec
import jsonrpc.backend.IdentityBackend
import jsonrpc.backend.IdentityBackend.Identity
import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.json.UpickleJsonCodec
import jsonrpc.transport.local.HandlerTransport
import jsonrpc.util.NoContext
import jsonrpc.{Client, Handler}
import ujson.Value

class NoContextClientHandlerSpec extends BaseSpec {

  "" - {
    "Construct" in {
      val codec = UpickleJsonCodec[UpickleCustom]()
      val backend = IdentityBackend()
      val handler = Handler.noContext(codec, backend)
      val handlerTransport = HandlerTransport(handler, backend, NoContext.value)
      val client = Client.noContext(codec, backend, handlerTransport)
      client
//      val result: String = client.callByPosition("test", 0)
//      val result: String = client.callByName("test", "foo" -> 0)
    }
  }
}

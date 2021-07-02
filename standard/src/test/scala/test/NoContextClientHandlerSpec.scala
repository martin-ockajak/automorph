package test

import base.BaseSpec
import jsonrpc.backend.IdentityBackend
import jsonrpc.backend.IdentityBackend.Identity
import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.json.UpickleJsonCodec
import jsonrpc.transport.local.HandlerTransport
import jsonrpc.util.Void
import jsonrpc.{Client, Handler}
import ujson.Value

class NoContextClientHandlerSpec extends BaseSpec {

  "" - {
    "Construct" in {
      val codec = UpickleJsonCodec[UpickleCustom]()
      val backend = IdentityBackend()
      val handler = Handler.basic[Value, codec.type, Identity](codec, backend)
      val handlerTransport = HandlerTransport(handler, backend, Void.value)
      val client = Client.basic[Value, codec.type, Identity](codec, backend, handlerTransport)
      client
//      val result: String = client.callByPosition("test", 0)
//      val result: String = client.callByName("test", "foo" -> 0)
    }
  }
}

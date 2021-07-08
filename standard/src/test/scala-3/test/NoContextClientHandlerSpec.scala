package test

import automorph.backend.IdentityBackend
import automorph.backend.IdentityBackend.Identity
import automorph.codec.common.UpickleCustom
import automorph.codec.json.UpickleJsonCodec
import automorph.transport.local.HandlerTransport
import automorph.util.NoContext
import automorph.{Client, Handler}
import test.base.BaseSpec
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

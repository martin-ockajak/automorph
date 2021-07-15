package test

import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.codec.common.UpickleCustom
import automorph.codec.json.UpickleJsonFormat
import automorph.transport.local.client.HandlerTransport
import automorph.util.EmptyContext
import automorph.{Client, Handler}
import test.base.BaseSpec

class EmptyContextCoreSpec extends BaseSpec {

  "" - {
    "Construct" in {
      val codec = UpickleJsonFormat[UpickleCustom]()
      val backend = IdentitySystem()
      val handler = Handler.withoutContext(codec, backend)
      val handlerTransport = HandlerTransport(handler, backend, EmptyContext.value)
      val client = Client.withoutContext(codec, backend, handlerTransport)
      client
//      val result: String = client.callByPosition("test", 0)
//      val result: String = client.callByName("test", "foo" -> 0)
    }
  }
}

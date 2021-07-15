package test

import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.format.json.UpickleJsonFormat
import automorph.format.UpickleCustom
import automorph.transport.local.client.HandlerTransport
import automorph.util.EmptyContext
import automorph.{Client, Handler}
import test.base.BaseSpec

class EmptyContextCoreSpec extends BaseSpec {

  "" - {
    "Construct" in {
      val format = UpickleJsonFormat[UpickleCustom]()
      val system = IdentitySystem()
      val handler = Handler.withoutContext(format, system)
      val handlerTransport = HandlerTransport(handler, system, EmptyContext.value)
      val client = Client.withoutContext(format, system, handlerTransport)
      client
//      val result: String = client.callByPosition("test", 0)
//      val result: String = client.callByName("test", "foo" -> 0)
    }
  }
}

package test

import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.format.json.CirceJsonFormat
import automorph.transport.local.client.HandlerTransport
import automorph.util.EmptyContext
import automorph.{Client, Handler}
import test.base.BaseSpec

class EmptyContextSpec extends BaseSpec {

  "" - {
    "Create" in {
      val format = CirceJsonFormat()
      val system = IdentitySystem()
      val handler = Handler.withoutContext(format, system)
      val handlerTransport = HandlerTransport(handler, system, EmptyContext.value)
      val client = Client.withoutContext(format, system, handlerTransport)
      client
    }
  }
}

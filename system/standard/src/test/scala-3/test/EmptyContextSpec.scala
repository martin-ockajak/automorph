package test

import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.codec.json.CirceJsonCodec
import automorph.transport.local.client.HandlerTransport
import automorph.util.EmptyContext
import automorph.{Client, Handler}
import test.base.BaseSpec

class EmptyContextSpec extends BaseSpec {

  "" - {
    "Create" in {
      val codec = CirceJsonCodec()
      val system = IdentitySystem()
      val handler = Handler.withoutContext(codec, system)
      val handlerTransport = HandlerTransport(handler, system, EmptyContext.value)
      val client = Client.withoutContext(codec, system, handlerTransport)
      client
    }
  }
}

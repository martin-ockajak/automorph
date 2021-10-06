package test

import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.codec.json.CirceJsonCodec
import automorph.transport.local.client.HandlerTransport
import automorph.util.EmptyContext
import automorph.{Client, Handler}
import ujson.Value

class EmptyContextSpec extends BaseSpec {

  "" - {
    "Create" in {
      val codec = CirceJsonCodec()
      val system = IdentitySystem()
      val handler = Handler.withoutContext[Value, codec.type, Identity](codec, system)
      val handlerTransport = HandlerTransport(handler, system, EmptyContext.default)
      val client = Client.withoutContext[Value, codec.type, Identity](codec, system, handlerTransport)
      client
    }
  }
}

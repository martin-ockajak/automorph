package test

import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.format.json.CirceJsonFormat
import automorph.transport.local.client.HandlerTransport
import automorph.util.EmptyContext
import automorph.{Client, Handler}
import ujson.Value

class EmptyContextSpec extends BaseSpec {

  "" - {
    "Create" in {
      val format = CirceJsonFormat()
      val system = IdentitySystem()
      val handler = Handler.withoutContext[Value, format.type, Identity](format, system)
      val handlerTransport = HandlerTransport(handler, system, EmptyContext.value)
      val client = Client.withoutContext[Value, format.type, Identity](format, system, handlerTransport)
      client
    }
  }
}

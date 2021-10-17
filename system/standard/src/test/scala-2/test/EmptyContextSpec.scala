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
      val protocol = JsonRpcProtocol(codec)
      val system = IdentitySystem()
      val handler = Handler.protocol(protocol).system(system).context[Context.Value]
      val transport = HandlerTransport(handler, system, EmptyContext.value)
      val client = Client.protocol(protocol).transport(transport)
      client
    }
  }
}

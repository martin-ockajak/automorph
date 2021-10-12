package test

import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.codec.json.CirceJsonCodec
import automorph.transport.local.client.HandlerTransport
import automorph.util.Context
import automorph.{Client, Handler}
import ujson.Value

class ContextSpec extends BaseSpec {

  "" - {
    "Create" in {
      val codec = CirceJsonCodec()
      val protocol = JsonRpcProtocol(codec)
      val system = IdentitySystem()
      val handler = Handler.protocol(protocol).system(system).context[Context.Empty]
      val transport = HandlerTransport(handler, system, Context.empty)
      val client = Client.protocol(protocol).transport(transport)
      client
    }
  }
}
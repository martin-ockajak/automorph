package test

import automorph.codec.json.CirceJsonCodec
import automorph.protocol.JsonRpcProtocol
import automorph.protocol.jsonrpc.Message
import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.transport.local.client.HandlerTransport
import automorph.util.Context
import automorph.{Client, Handler}
import io.circe.{Decoder, Encoder, Json}
import test.base.BaseSpec

class ContextSpec extends BaseSpec {

  "" - {
    "Create" in {
      val codec = CirceJsonCodec()
      val protocol = JsonRpcProtocol(codec)
      val system = IdentitySystem()
      val handler = Handler.protocol(protocol).system(system).context[Context.Empty]
      val handlerTransport = HandlerTransport(handler, system, Context.empty)
      val client = Client.protocol(protocol).transport(handlerTransport)
      client
    }
  }
}

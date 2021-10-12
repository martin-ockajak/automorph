package test

import automorph.codec.json.CirceJsonCodec
import automorph.protocol.JsonRpcProtocol
import automorph.protocol.jsonrpc.Message
import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.transport.local.client.HandlerTransport
import automorph.util.EmptyContext
import automorph.{Client, Handler}
import io.circe.{Decoder, Encoder, Json}
import test.base.BaseSpec

class EmptyContextSpec extends BaseSpec {

  "" - {
    "Create" in {
      val codec = CirceJsonCodec()
      val protocol = JsonRpcProtocol(codec)
      val system = IdentitySystem()
      val handler = Handler.protocol(protocol).system(system).context[EmptyContext.Value]
      val handlerTransport = HandlerTransport(handler, system, EmptyContext.default)
      val client = Client.protocol(protocol).transport(handlerTransport)
      client
    }
  }
}

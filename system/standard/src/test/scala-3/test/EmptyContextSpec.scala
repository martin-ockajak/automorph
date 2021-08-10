package test

import automorph.codec.json.{CirceJsonCodec, CirceJsonRpc}
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
      // FIXME
      given Encoder[Message[Json]] = CirceJsonRpc.messageEncoder
      given Decoder[Message[Json]] = CirceJsonRpc.messageDecoder

      val codec = CirceJsonCodec()
      val system = IdentitySystem()
      val protocol = JsonRpcProtocol(codec)
      val handler = Handler.withoutContext(codec, system, protocol)
      val handlerTransport = HandlerTransport(handler, system, EmptyContext.value)
      val client = Client.withoutContext(codec, system, handlerTransport, protocol)
      client
    }
  }
}

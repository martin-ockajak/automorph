package test

import automorph.codec.json.CirceJsonCodec
import automorph.handler.BindingHandler
import automorph.protocol.JsonRpcProtocol
import automorph.system.IdentitySystem
import automorph.transport.local.client.LocalClient
import automorph.{Client, EmptyContext}
import test.base.BaseTest

class EmptyContextTest extends BaseTest {

  "" - {
    "Create" in {
      val codec = CirceJsonCodec()
      val protocol = JsonRpcProtocol[CirceJsonCodec.Node, CirceJsonCodec, EmptyContext.Value](codec)
      val system = IdentitySystem()
      val handler = BindingHandler(protocol, system)
      val clientTransport = LocalClient(system, handler, EmptyContext.value)
      val client = Client.transport(clientTransport).rpcProtocol(protocol)
      client
    }
  }
}

//package test
//
//import automorph.codec.json.CirceJsonCodec
//import automorph.protocol.JsonRpcProtocol
//import automorph.system.IdentitySystem
//import automorph.transport.local.client.HandlerTransport
//import automorph.{Client, EmptyContext, Handler}
//import test.base.BaseTest
//
//class EmptyContextTest extends BaseTest {
//
//  "" - {
//    "Create" in {
//      val codec = CirceJsonCodec()
//      val protocol = JsonRpcProtocol[CirceJsonCodec.Node, CirceJsonCodec, EmptyContext.Value](codec)
//      val system = IdentitySystem()
//      val handler = Handler.protocol(protocol).system(system)
//      val transport = HandlerTransport(handler, system, EmptyContext.value)
//      val client = Client.protocol(protocol).transport(transport)
//      client
//    }
//  }
//}

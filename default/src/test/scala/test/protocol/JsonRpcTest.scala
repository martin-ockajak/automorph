package automorph.protocol

import automorph.Default
import test.base.BaseTest

class JsonRpcTest extends BaseTest {

  "" - {
    "API description" - {
      "OpenRPC" in {
        val protocol = JsonRpcProtocol[Default.Node, Default.Codec, Default.ServerContext](Default.codec)
        val description = protocol.apiDescriptions.find(_.function.name == JsonRpcProtocol.openRpcFunction)
        description.should(not(be(empty)))
      }
      "OpenAPI" in {
        val protocol = JsonRpcProtocol[Default.Node, Default.Codec, Default.ServerContext](Default.codec)
        val description = protocol.apiDescriptions.find(_.function.name == JsonRpcProtocol.openApiFunction)
        description.should(not(be(empty)))
      }
    }
  }
}

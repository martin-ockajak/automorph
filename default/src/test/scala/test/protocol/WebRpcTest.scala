package test.protocol

import automorph.Default
import automorph.protocol.WebRpcProtocol
import test.base.BaseTest

class WebRpcTest extends BaseTest {

  "" - {
    "API description" - {
      "OpenAPI" in {
        val protocol = WebRpcProtocol[Default.Node, Default.Codec, Default.ServerContext](Default.codec, "/api/")
        val description = protocol.apiSchemas.find(_.function.name == WebRpcProtocol.openApiFunction)
        description.should(not(be(empty)))
      }
    }
  }
}

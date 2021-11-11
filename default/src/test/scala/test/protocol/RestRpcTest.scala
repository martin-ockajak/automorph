package automorph.protocol

import automorph.Default
import test.base.BaseTest

class RestRpcTest extends BaseTest {

  "" - {
    "API description" - {
      "OpenAPI" in {
        val protocol = RestRpcProtocol[Default.Node, Default.Codec, Default.ServerContext](Default.codec, "/api/")
        val description = protocol.apiSchemas.find(_.function.name == RestRpcProtocol.openApiFunction)
        description.should(not(be(empty)))
      }
    }
  }
}

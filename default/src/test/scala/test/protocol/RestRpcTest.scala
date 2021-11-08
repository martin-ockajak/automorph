package automorph.protocol

import automorph.Default
import test.base.BaseTest

class RestRpcTest extends BaseTest {

  "" - {
    "API description" - {
      "OpenAPI" in {
        val protocol = RestRpcProtocol(Default.codec, "/api/").context[Default.ServerContext]
        val description = protocol.apiDescriptions.find(_.function.name == RestRpcProtocol.openApiFunction)
        description.should(not(be(empty)))
      }
    }
  }
}

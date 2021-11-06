package automorph.protocol

import automorph.Default
import automorph.spi.protocol.{RpcFunction, RpcParameter}
import test.base.BaseTest

class JsonRpcTest extends BaseTest {

  private val functions = Seq(
    RpcFunction(
      "test",
      Seq(
        RpcParameter("foo", "String"),
        RpcParameter("bar", "Integer"),
        RpcParameter("alt", "Option[Map[String, Boolean]")
      ),
      "Seq[String]",
      Some("Testing function")
    )
  )

  "" - {
    "Discovery" - {
      "OpenRPC" in {
        val protocol = JsonRpcProtocol[Default.Node, Default.Codec, Default.ServerContext](Default.codec)
        //      val specification = protocol.openApi(functions, "Test", "0.0", Seq("http://localhost:80/api"))
        //      specification.should(not(be(empty)))
      }
      "OpenAPI" in {
        val protocol = JsonRpcProtocol[Default.Node, Default.Codec, Default.ServerContext](Default.codec)
        //      val specification = protocol.openApi(functions, "Test", "0.0", Seq("http://localhost:80/api"))
        //      specification.should(not(be(empty)))
      }
    }
  }
}

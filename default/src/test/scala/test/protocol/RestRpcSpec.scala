package automorph.protocol

import automorph.DefaultMessageCodec
import automorph.spi.protocol.{RpcFunction, RpcParameter}
import io.circe.{Decoder, Encoder}
import test.base.BaseSpec

class RestRpcSpec extends BaseSpec {

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
    "OpenAPI" in {
      val protocol = RestRpcProtocol(
        DefaultMessageCodec(),
        RestRpcProtocol.defaultErrorToException,
        RestRpcProtocol.defaultExceptionToError
      )
      val specification = protocol.openApi(functions, "Test", "0.0", Seq("http://localhost:80/api"))
      specification.should(not(be(empty)))
    }
  }
}

package automorph.protocol

import automorph.DefaultMessageCodec
import automorph.codec.json.CirceJsonRpc
import automorph.spi.protocol.{RpcFunction, RpcParameter}
import io.circe.{Decoder, Encoder}
import test.base.BaseSpec

class JsonRpcSpec extends BaseSpec {

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

  // FIXME - remove
  private implicit val messageEncoder: Encoder[CirceJsonRpc.Data] = CirceJsonRpc.messageEncoder
  private implicit val messageDecoder: Decoder[CirceJsonRpc.Data] = CirceJsonRpc.messageDecoder

  "" - {
    "OpenApi" in {
      val protocol = JsonRpcProtocol(
        DefaultMessageCodec(),
        JsonRpcProtocol.defaultErrorToException,
        JsonRpcProtocol.defaultExceptionToError
      )
      val specification = protocol.openApi(functions, "Test", "0.0", Seq("http://localhost:80/api"))
      specification.should(not(be(empty)))
    }
  }
}

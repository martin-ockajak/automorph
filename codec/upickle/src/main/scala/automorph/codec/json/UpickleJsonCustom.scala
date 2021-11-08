package automorph.codec.json

import automorph.codec.UpickleCustom
import automorph.description.{OpenApi, OpenRpc}

/** Basic null-safe data types and RPC protocol message support for uPickle message codec using JSON format. */
trait UpickleJsonCustom extends UpickleCustom {

  implicit lazy val jsonRpcMessageRw: ReadWriter[UpickleJsonRpc.RpcMessage] =
    UpickleJsonRpc.readWriter(this)

  implicit lazy val restRpcMessageRw: ReadWriter[UpickleRestRpc.RpcMessage] =
    UpickleRestRpc.readWriter(this)

  implicit lazy val openRpcRw: ReadWriter[OpenRpc] = UpickleOpenRpc.readWriter(this)
  implicit lazy val openApiRw: ReadWriter[OpenApi] = UpickleOpenApi.readWriter(this)
}

object UpickleJsonCustom {
  /** Default data types support for uPickle message codec using JSON format. */
  lazy val default = new UpickleJsonCustom {}
}

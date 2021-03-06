package automorph.codec.messagepack

import automorph.codec.UpickleCustom
import automorph.schema.{OpenApi, OpenRpc}

/** Basic null-safe data types and RPC protocol message support for uPickle message codec using MessagePack format. */
trait UpickleMessagePackCustom extends UpickleCustom {

  implicit lazy val jsonRpcMessageRw: ReadWriter[UpickleJsonRpc.RpcMessage] =
    UpickleJsonRpc.readWriter(this)

  implicit lazy val restRpcMessageRw: ReadWriter[UpickleWebRpc.RpcMessage] =
    UpickleWebRpc.readWriter(this)

  implicit lazy val openRpcRw: ReadWriter[OpenRpc] = UpickleOpenRpc.readWriter(this)
  implicit lazy val openApiRw: ReadWriter[OpenApi] = UpickleOpenApi.readWriter(this)
}

object UpickleMessagePackCustom {
  /** Default data types support for uPickle message codec using MessagePack format. */
  lazy val default = new UpickleMessagePackCustom {}
}

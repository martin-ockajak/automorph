package automorph.codec.json

import automorph.codec.UpickleCustom

/**
 * Basic null-safe data types and RPC protocol message support for uPickle message codec using JSON format.
 */
trait UpickleJsonCustom extends UpickleCustom {

  implicit lazy val jsonRpcMessageRw: ReadWriter[UpickleJsonRpc.RpcMessage] =
    UpickleJsonRpc.readWriter(this)

  implicit lazy val restRpcMessageRw: ReadWriter[UpickleRestRpc.RpcMessage] =
    UpickleRestRpc.readWriter(this)
}

object UpickleJsonCustom {
  /** Default data types support for uPickle message codec using JSON format. */
  lazy val default = new UpickleJsonCustom {}
}

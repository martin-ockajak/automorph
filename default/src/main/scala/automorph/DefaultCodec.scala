package automorph

import automorph.codec.common.DefaultUpickleCustom
import automorph.codec.json.UpickleJsonCodec

case object DefaultCodec {

  /** Default message node type. */
  type Node = UpickleJsonCodec.Node

  /** Default message codec plugin type. */
  type Type = UpickleJsonCodec[DefaultUpickleCustom.type]

  /**
   * Creates a default structured message format codec plugin.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @return codec plugin
   */
  def apply(): Type = UpickleJsonCodec()
}

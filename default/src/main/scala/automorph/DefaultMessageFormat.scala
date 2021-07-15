package automorph

import automorph.codec.common.DefaultUpickleCustom
import automorph.codec.json.UpickleJsonFormat

case object DefaultMessageFormat {

  /** Default message node type. */
  type Node = UpickleJsonFormat.Node

  /** Default message codec plugin type. */
  type Type = UpickleJsonFormat[DefaultUpickleCustom.type]

  /**
   * Creates a default structured message format codec plugin.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @return codec plugin
   */
  def apply(): Type = UpickleJsonFormat()
}

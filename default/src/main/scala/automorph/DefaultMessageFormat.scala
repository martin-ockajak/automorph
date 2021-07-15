package automorph

import automorph.format.json.UpickleJsonFormat
import automorph.format.DefaultUpickleCustom

case object DefaultMessageFormat {

  /** Default message node type. */
  type Node = UpickleJsonFormat.Node

  /** Default message format plugin type. */
  type Type = UpickleJsonFormat[DefaultUpickleCustom.type]

  /**
   * Creates a default structured message format format plugin.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @return format plugin
   */
  def apply(): Type = UpickleJsonFormat()
}

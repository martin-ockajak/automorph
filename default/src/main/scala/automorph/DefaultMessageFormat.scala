package automorph

import automorph.format.json.CirceJsonFormat

case object DefaultMessageFormat {

  /** Default message node type. */
  type Node = CirceJsonFormat.Node

  /** Default message format plugin type. */
  type Type = CirceJsonFormat

  /**
   * Creates a default structured message format format plugin.
   *
   * @return message format plugin
   */
  def apply(): Type = CirceJsonFormat()
}

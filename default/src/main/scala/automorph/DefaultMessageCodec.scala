package automorph

import automorph.codec.json.CirceJsonCodec

object DefaultMessageCodec {

  /** Default message node type. */
  type Node = CirceJsonCodec.Node

  /** Default message codec plugin type. */
  type Type = CirceJsonCodec

  /**
   * Creates a default message ''codec'' plugin.
   *
   * @return message ''codec'' plugin
   */
  def apply(): Type = CirceJsonCodec()
}

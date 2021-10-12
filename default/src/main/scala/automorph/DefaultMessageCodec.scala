package automorph

import automorph.codec.json.CirceJsonCodec

object DefaultMessageCodec {

  /** Default message node type. */
  type Node = CirceJsonCodec.Node

  /** Default message codec plugin type. */
  type Type = CirceJsonCodec

  /**
   * Creates a default JSON message codec plugin.
   *
   * @see [[https://www.json.org Format]]
   * @see [[https://circe.github.io/circe Documentation]]
   * @see [[https://circe.github.io/circe/api/io/circe/Json.html Node type]]
   * @return message codec plugin
   */
  def apply(): Type = CirceJsonCodec()
}

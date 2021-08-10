package automorph.openapi

import automorph.codec.json.CirceJsonCodec.Node

final case class Example (
  summary: Option[String] = None,
  description: Option[String] = None,
  value: Option[Node] = None
)

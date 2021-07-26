package automorph.openapi

import automorph.spi.Message
import automorph.format.json.CirceJsonFormat.Node

final case class Example (
  summary: Option[String],
  description: Option[String],
  value: Message[Node]
)

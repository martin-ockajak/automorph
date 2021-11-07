package automorph.specification.openapi

case class ServerVariable(
  `enum`: Option[List[String]],
  default: String,
  description: Option[String] = None
)

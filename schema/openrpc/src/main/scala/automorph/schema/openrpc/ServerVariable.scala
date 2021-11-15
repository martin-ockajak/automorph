package automorph.schema.openrpc

case class ServerVariable(
  `enum`: Option[List[String]],
  default: String,
  description: Option[String] = None
)

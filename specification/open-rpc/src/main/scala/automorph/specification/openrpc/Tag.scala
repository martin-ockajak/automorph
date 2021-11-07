package automorph.specification.openrpc

case class Tag(
  name: String,
  summary: Option[String] = None,
  description: Option[String] = None,
  externalDocs: Option[ExternalDocumentation] = None
)

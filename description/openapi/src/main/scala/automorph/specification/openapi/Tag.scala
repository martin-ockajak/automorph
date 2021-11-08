package automorph.description.openapi

case class Tag(
  name: String,
  description: Option[String] = None,
  externalDocs: Option[ExternalDocumentation] = None
)

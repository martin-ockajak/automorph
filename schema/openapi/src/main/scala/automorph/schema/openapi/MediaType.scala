package automorph.schema.openapi

case class MediaType(
  schema: Option[Schema] = None,
  example: Option[String] = None,
  examples: Option[Map[String, Example]] = None,
  encoding: Option[Map[String, Encoding]] = None
)

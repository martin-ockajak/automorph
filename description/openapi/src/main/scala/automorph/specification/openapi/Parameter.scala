package automorph.description.openapi

case class Parameter(
  name: String,
  in: String,
  descriptipon: Option[String] = None,
  required: Option[Boolean] = None,
  deprecated: Option[Boolean] = None,
  allowEmptyValue: Option[Boolean] = None,
  style: Option[String] = None,
  explode: Option[Boolean] = None,
  allowReserved: Option[Boolean] = None,
  schema: Option[Schema] = None,
  example: Option[String] = None,
  examples: Option[Map[String, Example]] = None,
  content: Option[Map[String, MediaType]] = None,
  $ref: Option[String] = None
) extends Reference

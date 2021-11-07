package automorph.specification.openapi

final case class RequestBody(
  description: Option[String] = None,
  content: Map[String, MediaType],
  required: Option[Boolean] = None
)

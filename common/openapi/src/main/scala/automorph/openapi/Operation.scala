package automorph.openapi

final case class Operation(
  requestBody: Option[RequestBody] = None,
  responses: Option[Map[String, Response]] = None,
  summary: Option[String] = None,
  description: Option[String] = None
)

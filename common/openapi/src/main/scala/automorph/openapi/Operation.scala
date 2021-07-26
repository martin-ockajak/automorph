package automorph.openapi

final case class Operation(
  requestBody: RequestBody,
  responses: Map[String, Response],
  summary: Option[String],
  description: Option[String]
)

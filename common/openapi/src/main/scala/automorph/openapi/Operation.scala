package automorph.openapi

import automorph.openapi.Operation.Responses

final case class Operation(
  requestBody: Option[RequestBody] = None,
  responses: Option[Responses] = None,
  summary: Option[String] = None,
  description: Option[String] = None
)

case object Operation {
  type Responses = Map[String, Response]
}

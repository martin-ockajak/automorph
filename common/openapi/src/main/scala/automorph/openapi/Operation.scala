package automorph.openapi

import automorph.openapi.Operation.Responses

private [automorph] final case class Operation(
  requestBody: Option[RequestBody] = None,
  responses: Option[Responses] = None,
  summary: Option[String] = None,
  description: Option[String] = None
)

private [automorph] case object Operation {
  type Responses = Map[String, Response]
}

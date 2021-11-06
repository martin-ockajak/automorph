package automorph.openapi

import automorph.openapi.Operation.Responses

private [automorph] final case class Operation(
  requestBody: Option[RequestBody] = None,
  responses: Option[Responses] = None,
  summary: Option[String] = None,
  description: Option[String] = None
) {
  def map: Map[String, Any] = Map(
    "requestBody" -> requestBody.map(_.map),
    "responses" -> responses.map(_.view.mapValues(_.map).toMap),
    "summary" -> summary,
    "description" -> description
  )
}

private [automorph] object Operation {
  type Responses = Map[String, Response]
}

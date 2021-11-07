package automorph.specification.openapi

import automorph.specification.jsonschema.Reference
import automorph.specification.openapi.Operation.{Callback, Responses, SecurityRequirement}

case class Operation(
  tags: Option[List[String]] = None,
  summary: Option[String] = None,
  description: Option[String] = None,
  externalDocs: Option[ExternalDocumentation] = None,
  operationId: Option[String] = None,
  parameters: Option[List[Either[Parameter, Reference]]] = None,
  requestBody: Option[Either[RequestBody, Reference]] = None,
  responses: Responses,
  callbacks: Option[Map[String, Either[Callback, Reference]]] = None,
  deprecated: Option[Boolean] = None,
  security: Option[List[SecurityRequirement]] = None,
  servers: Option[List[Server]] = None
)

object Operation {
  type Responses = Map[String, Either[Response, Reference]]
  type Callback = Map[String, PathItem]
  type SecurityRequirement = Map[String, List[String]]
}

package automorph.specification.openapi

import automorph.specification.jsonschema.{Reference, Schema}
import automorph.specification.openapi.Operation.Callback

case class Components(
  schemas: Option[Map[String, Schema]] = None,
  responses: Option[Map[String, Response]] = None,
  parameters: Option[Map[String, Parameter]] = None,
  examples: Option[Map[String, Example]] = None,
  requestBodies: Option[Map[String, RequestBody]] = None,
  headers: Option[Map[String, Header]] = None,
  securitySchemes: Option[Map[String, SecurityScheme]] = None,
  links: Option[Map[String, Link]] = None,
  callbacks: Option[Map[String, Callback]] = None,
  pathItems: Option[Map[String, PathItem]] = None
)

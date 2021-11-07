package automorph.specification.openapi

import automorph.specification.jsonschema.{Reference, Schema}
import automorph.specification.openapi.Operation.Callback

case class Components(
  schemas: Option[Map[String, Either[Schema, Reference]]] = None,
  responses: Option[Map[String, Either[Response, Reference]]] = None,
  parameters: Option[Map[String, Either[Parameter, Reference]]] = None,
  examples: Option[Map[String, Either[Example, Reference]]] = None,
  requestBodies: Option[Map[String, Either[RequestBody, Reference]]] = None,
  headers: Option[Map[String, Either[Header, Reference]]] = None,
  securitySchemes: Option[Map[String, Either[SecurityScheme, Reference]]] = None,
  links: Option[Map[String, Either[Link, Reference]]] = None,
  callbacks: Option[Map[String, Either[Callback, Reference]]] = None,
  pathItems: Option[Map[String, Either[PathItem, Reference]]] = None
)

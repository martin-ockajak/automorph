package automorph.specification.openapi

import automorph.specification.jsonschema.{Reference, Schema}

case class MediaType (
  schema: Option[Either[Schema, Reference]] = None,
  example: Option[String] = None,
  examples: Option[Map[String, Either[Example, Reference]]] = None,
  encoding: Option[Map[String, Encoding]] = None
)

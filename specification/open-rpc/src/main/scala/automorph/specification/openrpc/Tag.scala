package automorph.specification.openrpc

import automorph.specification.jsonschema.Reference

case class Tag(
  name: String,
  summary: Option[String] = None,
  description: Option[String] = None,
  externalDocs: Option[ExternalDocumentation] = None,
  $ref: Option[String] = None
) extends Reference

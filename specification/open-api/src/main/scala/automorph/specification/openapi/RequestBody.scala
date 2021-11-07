package automorph.specification.openapi

import automorph.specification.jsonschema.Reference

final case class RequestBody(
  description: Option[String] = None,
  content: Map[String, MediaType],
  required: Option[Boolean] = None,
  $ref: Option[String] = None
) extends Reference

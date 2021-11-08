package automorph.description.openapi

import automorph.description.jsonschema.Reference

final case class RequestBody(
  description: Option[String] = None,
  content: Map[String, MediaType],
  required: Option[Boolean] = None,
  $ref: Option[String] = None
) extends Reference

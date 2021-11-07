package automorph.specification.openapi

import automorph.specification.jsonschema.Reference

final case class Response(
  description: String,
  headers: Option[Map[String, Header]] = None,
  content: Option[Map[String, MediaType]] = None,
  links: Option[Map[String, Link]] = None,
  $ref: Option[String] = None
) extends Reference

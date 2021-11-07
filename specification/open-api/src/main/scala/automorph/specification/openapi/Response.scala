package automorph.specification.openapi

import automorph.specification.jsonschema.Reference

final case class Response(
  description: String,
  headers: Option[Map[String, Either[Header, Reference]]] = None,
  content: Option[Map[String, MediaType]] = None,
  links: Option[Map[String, Either[Link, Reference]]] = None
)

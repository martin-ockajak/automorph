package automorph.specification.openrpc

import automorph.specification.jsonschema.Schema

case class ContentDescriptor(
  name: String,
  summary: Option[String] = None,
  description: Option[String] = None,
  required: Option[Boolean] = None,
  schema: Schema,
  deprecated: Option[Boolean] = None
)

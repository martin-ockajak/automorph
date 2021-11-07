package automorph.specification.openapi

import automorph.specification.jsonschema.Reference

case class Example(
  summary: Option[String] = None,
  description: Option[String] = None,
  value: Option[String],
  externalValue: Option[String] = None,
  $ref: Option[String] = None
) extends Reference

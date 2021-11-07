package automorph.specification.openapi

import automorph.specification.jsonschema.Reference

case class Link(
  operationRef: Option[String] = None,
  operationId: Option[String] = None,
  parameters: Option[Map[String, String]] = None,
  requestBody: Option[String] = None,
  description: Option[String] = None,
  server: Option[Server] = None,
  $ref: Option[String] = None
) extends Reference

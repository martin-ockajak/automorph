package automorph.openapi

import automorph.openapi.Schema.Properties

final case class Schema(
  `type`: Option[String] = None,
  title: Option[String] = None,
  description: Option[String] = None,
  properties: Option[Properties] = None,
  required: Option[List[String]] = None,
  default: Option[String] = None,
  allOf: Option[List[Schema]] = None,
  $ref: Option[String] = None
)

case object Schema {
  type Properties = Map[String, Schema]
}
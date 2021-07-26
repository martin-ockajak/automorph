package automorph.openapi

import automorph.openapi.Schema.Properties

final case class Schema(
  title: Option[String] = None,
  `type`: Option[String] = None,
  properties: Option[Properties] = None,
  required: Option[List[String]] = None,
  $ref: Option[String] = None,
  allOf: Option[List[Schema]] = None
)

case object Schema {
  type Properties = Map[String, Property]
}
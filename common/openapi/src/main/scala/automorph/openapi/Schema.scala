package automorph.openapi

import automorph.openapi.Schema.Properties

final case class Schema(
  title: Option[String],
  `type`: String,
  properties: Properties,
  required: Option[List[String]],
  $ref: Option[String],
  allOf: Option[List[Schema]]
)

case object Schema {
  type Properties = Map[String, Property]
}
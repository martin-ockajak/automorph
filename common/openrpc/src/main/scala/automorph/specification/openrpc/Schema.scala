package automorph.specification.openrpc

import automorph.specification.openrpc.Schema.Properties

private [automorph] final case class Schema(
  `type`: Option[String] = None,
  title: Option[String] = None,
  description: Option[String] = None,
  properties: Option[Properties] = None,
  required: Option[List[String]] = None,
  default: Option[String] = None,
  allOf: Option[List[Schema]] = None,
  $ref: Option[String] = None
) {
  def map: Map[String, Any] = Map(
    "type" -> `type`,
    "title" -> title,
    "description" -> description,
    "properties" -> properties.map(_.view.mapValues(_.map).toMap),
    "required" -> required,
    "default" -> default,
    "allOf" -> allOf.map(_.map(_.map)),
    "$ref" -> $ref
  )
}

private [automorph] object Schema {
  type Properties = Map[String, Schema]
}

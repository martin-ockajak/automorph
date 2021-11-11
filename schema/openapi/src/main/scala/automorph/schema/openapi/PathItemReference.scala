package automorph.schema.openapi

case class PathItemReference(
  $ref: Option[String]
) extends Reference

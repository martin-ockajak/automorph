package automorph.description.openapi

case class PathItemReference(
  $ref: Option[String]
) extends Reference

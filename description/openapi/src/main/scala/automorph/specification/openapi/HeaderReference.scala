package automorph.description.openapi

case class HeaderReference(
  $ref: Option[String]
) extends Reference

package automorph.openapi

final case class Property(
  `type`: String,
  default: Option[String] = None,
  title: Option[String] = None,
  description: Option[String] = None
)

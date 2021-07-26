package automorph.openapi

final case class License(
  name: String,
  identifier: Option[String] = None,
  url: Option[String] = None
)

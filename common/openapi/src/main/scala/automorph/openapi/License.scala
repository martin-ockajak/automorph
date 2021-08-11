package automorph.openapi

private[automorph] final case class License(
  name: String,
  identifier: Option[String] = None,
  url: Option[String] = None
)

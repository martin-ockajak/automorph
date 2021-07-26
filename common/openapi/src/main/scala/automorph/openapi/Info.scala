package automorph.openapi

final case class Info(
  title: String,
  version: String,
  summary: Option[String] = None,
  description: Option[String] = None,
  termsOfService: Option[String] = None,
  contact: Option[Contact] = None,
  license: Option[License] = None
)

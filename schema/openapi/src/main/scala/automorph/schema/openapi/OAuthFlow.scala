package automorph.schema.openapi

case class OAuthFlow(
  authorizationUrl: Option[String] = None,
  tokenUrl: Option[String] = None,
  refreshUrl: Option[String] = None,
  scopes: Option[Map[String, String]] = None
)

package automorph.specification.openapi

case class SecurityScheme(
  `type`: String,
  description: Option[String] = None,
  name: Option[String] = None,
  in: Option[String] = None,
  scheme: Option[String] = None,
  bearerFormat: Option[String] = None,
  flows: Option[OAuthFlows] = None,
  openIdConnectUrl: Option[String] = None
)

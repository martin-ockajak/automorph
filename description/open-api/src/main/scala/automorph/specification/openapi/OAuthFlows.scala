package automorph.description.openapi

case class OAuthFlows(
  `implicit`: Option[OAuthFlow] = None,
  password: Option[OAuthFlow] = None,
  clientCredentials: Option[OAuthFlow] = None,
  authorizationCode: Option[OAuthFlow] = None,
)

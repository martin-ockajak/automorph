package automorph.specification.openapi

case class Server(
  url: String,
  description: Option[String] = None,
  variables: Option[Map[String, String]] = None
)
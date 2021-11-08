package automorph.description.openrpc

case class Server(
  name: String,
  url: String,
  summary: Option[String] = None,
  description: Option[String] = None,
  variables: Option[Map[String, String]] = None
)

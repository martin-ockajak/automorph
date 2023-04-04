package automorph.schema.openapi

case class Server(
  url: String,
  description: Option[String] = None,
  variables: Option[Map[String, ServerVariable]] = None,
)

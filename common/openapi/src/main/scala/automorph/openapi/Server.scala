package automorph.openapi

private [automorph] final case class Server(
  url: String,
  description: Option[String] = None,
  variables: Option[Map[String, String]] = None
)

package automorph.specification.openapi

case class Link(
  operationRef: Option[String] = None,
  operationId: Option[String] = None,
  parameters: Option[Map[String, String]] = None,
  requestBody: Option[String] = None,
  description: Option[String] = None,
  server: Option[Server] = None
)

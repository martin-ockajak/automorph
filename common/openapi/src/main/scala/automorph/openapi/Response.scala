package automorph.openapi

import automorph.openapi.Response.Content

final case class Response(
  description: String,
  content: Option[Content]
)

case object Response {
  type Content = Map[String, MediaType]
}

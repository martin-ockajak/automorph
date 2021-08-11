package automorph.openapi

import automorph.openapi.Response.Content

private [automorph] final case class Response(
  description: String,
  content: Option[Content]
)

private [automorph] case object Response {
  type Content = Map[String, MediaType]
}

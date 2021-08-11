package automorph.openapi

import automorph.openapi.RequestBody.Content

private [automorph] final case class RequestBody(
  content: Content,
  required: Option[Boolean] = None,
  description: Option[String] = None
)

private [automorph] case object RequestBody {
  type Content = Map[String, MediaType]
}

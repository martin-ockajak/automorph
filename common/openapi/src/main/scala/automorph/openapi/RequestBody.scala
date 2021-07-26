package automorph.openapi

import RequestBody.Content

final case class RequestBody(
  content: Content,
  required: Option[Boolean] = None,
  description: Option[String] = None
)

case object RequestBody {
  type Content = Map[String, MediaType]
}
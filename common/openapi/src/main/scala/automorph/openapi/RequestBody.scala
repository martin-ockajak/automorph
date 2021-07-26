package automorph.openapi

import RequestBody.Content

final case class RequestBody(
  content: Content,
  required: Option[Boolean],
  description: Option[String]
)

case object RequestBody {
  type Content = Map[String, MediaType]
}
package automorph.openapi

import automorph.openapi.RequestBody.Content

private [automorph] final case class RequestBody(
  content: Content,
  required: Option[Boolean] = None,
  description: Option[String] = None
) {
  def map: Map[String, Any] = Map(
    "content" -> content.view.mapValues(_.map).toMap,
    "required" -> required,
    "description" -> description
  )
}

private [automorph] object RequestBody {
  type Content = Map[String, MediaType]
}

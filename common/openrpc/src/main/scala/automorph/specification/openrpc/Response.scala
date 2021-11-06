package automorph.specification.openrpc

import automorph.specification.openrpc.Response.Content

private [automorph] final case class Response(
  description: String,
  content: Option[Content]
) {
  def map: Map[String, Any] = Map(
    "description" -> description,
    "content" -> content.map(_.view.mapValues(_.map).toMap)
  )
}

private [automorph] object Response {
  type Content = Map[String, MediaType]
}

package automorph.specification.openrpc

private [automorph] final case class PathItem(
  get: Option[Operation] = None,
  put: Option[Operation] = None,
  post: Option[Operation] = None,
  delete: Option[Operation] = None,
  parameters: Option[List[Parameter]] = None,
  summary: Option[String] = None,
  description: Option[String] = None
) {
  def map: Map[String, Any] = Map(
    "get" -> get.map(_.map),
    "put" -> put.map(_.map),
    "post" -> post.map(_.map),
    "delete" -> delete.map(_.map),
    "parameters" -> parameters.map(_.map(_.map)),
    "summary" -> summary,
    "description" -> description
  )
}

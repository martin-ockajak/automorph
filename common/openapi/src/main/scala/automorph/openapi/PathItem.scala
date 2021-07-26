package automorph.openapi

final case class PathItem(
  get: Option[Operation] = None,
  put: Option[Operation] = None,
  post: Option[Operation] = None,
  delete: Option[Operation] = None,
  parameters: Option[List[Parameter]] = None,
  summary: Option[String] = None,
  description: Option[String] = None
)

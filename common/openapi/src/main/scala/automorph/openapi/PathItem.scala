package automorph.openapi

final case class PathItem(
  get: Option[Operation],
  put: Option[Operation],
  post: Option[Operation],
  delete: Option[Operation],
  parameters: Option[List[Parameter]],
  summary: Option[String],
  description: Option[String]
)

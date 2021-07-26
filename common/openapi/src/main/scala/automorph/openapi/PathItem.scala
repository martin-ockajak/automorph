package automorph.openapi

final case class PathItem(
  get: Option[Operation],
  put: Option[Operation],
  post: Option[Operation],
  delete: Option[Operation]
)

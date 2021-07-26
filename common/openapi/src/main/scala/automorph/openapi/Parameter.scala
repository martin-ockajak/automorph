package automorph.openapi

final case class Parameter(
  name: String,
  in: String,
  required: Boolean,
  description: Option[String] = None
)

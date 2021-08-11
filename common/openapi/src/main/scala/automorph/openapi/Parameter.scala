package automorph.openapi

private [automorph] final case class Parameter(
  name: String,
  in: String,
  required: Boolean,
  description: Option[String] = None
)

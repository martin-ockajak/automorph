package automorph.specification.openrpc

private [automorph] final case class Parameter(
  name: String,
  in: String,
  required: Boolean,
  description: Option[String] = None
) {
  def map: Map[String, Any] = Map(
    "name" -> name,
    "in" -> in,
    "required" -> required,
    "description" -> description
  )
}

package automorph.specification.openrpc

private [automorph] final case class MediaType (
  schema: Option[Schema] = None
) {
  def map: Map[String, Any] = Map(
    "schema" -> schema.map(_.map)
  )
}

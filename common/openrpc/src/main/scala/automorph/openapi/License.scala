package automorph.openapi

final private[automorph] case class License(
  name: String,
  identifier: Option[String] = None,
  url: Option[String] = None
) {

  def map: Map[String, Any] = Map(
    "name" -> name,
    "identifier" -> identifier,
    "url" -> url
  )
}

package automorph.openapi

final private[automorph] case class Info(
  title: String,
  version: String,
  summary: Option[String] = None,
  description: Option[String] = None,
  termsOfService: Option[String] = None,
  contact: Option[Contact] = None,
  license: Option[License] = None
) {

  def map: Map[String, Any] = Map(
    "title" -> title,
    "version" -> version,
    "summary" -> summary,
    "description" -> description,
    "termsOfService" -> termsOfService,
    "contact" -> contact.map,
    "license" -> license.map
  )
}

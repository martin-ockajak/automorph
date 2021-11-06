package automorph.specification.openapi

final private[automorph] case class Contact(
  name: Option[String],
  url: Option[String],
  email: Option[String]
) {

  def map: Map[String, Any] = Map(
    "name" -> name,
    "url" -> url,
    "email" -> email
  )
}

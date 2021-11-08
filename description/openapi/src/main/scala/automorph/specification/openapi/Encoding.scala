package automorph.description.openapi

case class Encoding(
  contentType: Option[String] = None,
  headers: Option[Map[String, Header]],
  style: Option[String] = None,
  explode: Option[Boolean] = None,
  allowReserved: Option[Boolean] = None
)

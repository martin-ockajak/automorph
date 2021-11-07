package automorph.specification.openapi

import automorph.specification.jsonschema.{Reference, Schema}

case class Encoding(
  contentType: Option[String] = None,
  headers: Option[Map[String, Either[Header, Reference]]],
  style: Option[String] = None,
  explode: Option[Boolean] = None,
  allowReserved: Option[Boolean] = None
)

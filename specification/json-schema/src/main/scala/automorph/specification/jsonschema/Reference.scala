package automorph.specification.jsonschema

trait Reference {
  def $ref: Option[String]
}

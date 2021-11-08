package automorph.description.jsonschema

trait Reference {
  def $ref: Option[String]
}

package automorph.description.openapi

trait Reference {
  def $ref: Option[String]
}

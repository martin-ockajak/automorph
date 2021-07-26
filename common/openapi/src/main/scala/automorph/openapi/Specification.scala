package automorph.openapi

/**
 * Open API specification.
 *
 * @see https://swagger.io/specification/
 */
final case class Specification(
  xSendDefaults: Boolean = true,
  openapi: String = "3.0.0",
  info: Info,
  paths: Paths,
  xApiId: String
)

case object Specification {
  type Paths = Map[String, PathItem]
}

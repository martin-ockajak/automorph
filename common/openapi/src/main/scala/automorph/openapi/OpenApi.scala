package automorph.openapi

/**
 * Open API specification.
 *
 * @see https://swagger.io/specification/
 */
final case class Specification(
  openapi: String = "3.1.0",
  info: Info,
  paths: Paths
)

case object Specification {
  type Paths = Map[String, PathItem]
}

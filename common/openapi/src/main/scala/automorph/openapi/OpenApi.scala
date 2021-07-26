package automorph.openapi

/**
 * Open API specification.
 *
 * @see https://swagger.io/specification/
 */
final case class OpenApi(
  openapi: String = "3.1.0",
  info: Info,
  servers: List[Server],
  paths: Paths,
  components: Components
)

case object OpenApi {
  type Paths = Map[String, PathItem]
  type Components = Map[String, Schema]
}

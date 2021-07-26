package automorph.openapi

import automorph.openapi.Specification.{Components, Paths, Servers}

/**
 * Open API specification.
 *
 * @see https://github.com/OAI/OpenAPI-Specification
 */
final case class Specification(
  openapi: String = "3.1.0",
  info: Info,
  servers: Option[Servers] = None,
  paths: Option[Paths] = None,
  components: Option[Components] = None
)

case object Specification {
  type Servers = List[Server]
  type Paths = Map[String, PathItem]
  type Components = Map[String, Schema]
}

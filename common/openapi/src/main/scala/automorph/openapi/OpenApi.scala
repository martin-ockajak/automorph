package automorph.openapi

import automorph.openapi.OpenApi.{Components, Paths}

/**
 * Open API specification.
 *
 * @see https://github.com/OAI/OpenAPI-Specification
 */
final case class OpenApi(
  openapi: String = "3.1.0",
  info: Info,
  servers: Option[List[Server]] = None,
  paths: Option[Paths] = None,
  components: Option[Components] = None
)

case object OpenApi {
  type Paths = Map[String, PathItem]
  type Components = Map[String, Schema]
}

package automorph.openapi

import automorph.openapi.Specification.{Components, Paths, Servers}

/**
 * Open API specification.
 *
 * @see https://github.com/OAI/OpenAPI-Specification
 */
private [automorph] final case class Specification(
  openapi: String = "3.1.0",
  info: Info,
  servers: Option[Servers] = None,
  paths: Option[Paths] = None,
  components: Option[Components] = None
) {
  def json: String = Json.map(Map(
    "openapi" -> openapi,
    "info" -> info.map,
    "servers" -> servers.map(_.map),
    "paths" -> paths.map(_.view.mapValues(_.map).toMap),
    "components" -> components.map(_.view.mapValues(_.map).toMap)
  ), 0, 0)
}

private [automorph] case object Specification {
  type Servers = List[Server]
  type Paths = Map[String, PathItem]
  type Components = Map[String, Schema]
}

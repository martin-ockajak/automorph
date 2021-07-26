package automorph.openapi

import automorph.openapi.Specification.{Components, Paths, Servers}
import automorph.util.Method

/**
 * Open API specification generator.
 *
 * @see https://github.com/OAI/OpenAPI-Specification
 */
case object Generator {

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param info OpenAPI Info object
   * @param servers OpenAPI Server objects
   * @return OpenAPI specification
   */
  def jsonRpc(methods: Map[String, Method], info: Info, servers: Seq[Server]): Specification = {
    val paths = toPaths(methods, true)
    val components = toComponents()
    Specification(info = info, servers = toServers(servers), paths = paths, components = components)
  }

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param title API title
   * @param version API specification version
   * @param serverUrls API server URL
   * @return OpenAPI specification
   */
  def jsonRpc(methods: Map[String, Method], title: String, version: String, serverUrls: Seq[String]): Specification =
    jsonRpc(methods, Info(title = title, version = version), toServers(serverUrls))

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param info OpenAPI Info object
   * @param servers OpenAPI Server objects
   * @return OpenAPI specification
   */
  def restRpc(methods: Map[String, Method], info: Info, servers: Seq[Server]): Specification = {
    val paths = toPaths(methods, false)
    val components = toComponents()
    Specification(info = info, servers = toServers(servers), paths = paths, components = components)
  }

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param title API title
   * @param version API specification version
   * @param serverUrls API server URL
   * @return OpenAPI specification
   */
  def restRpc(methods: Map[String, Method], title: String, version: String, serverUrls: Seq[String]): Specification =
    restRpc(methods, Info(title = title, version = version), toServers(serverUrls))

  private def toServers(serverUrls: Seq[String]): Seq[Server] = serverUrls.map(url => Server(url = url))

  private def toServers(servers: Seq[Server]): Option[Servers] = servers match {
    case Seq() => None
    case someServers => Some(someServers.toList)
  }

  private def toPaths(methods: Map[String, Method], rpc: Boolean): Option[Paths] = methods match {
    case noMethods if noMethods.isEmpty => None
    case someMethods =>
      Some(someMethods.map { case (name, method) =>
        val path = s"/${name.replace('.', '/')}"
        val pathItem = if (rpc) jsonRpcPathItem(method) else restRpcPathItem(method)
        path -> pathItem
      }.toMap)
  }

  private def jsonRpcPathItem(method: Method): PathItem =
    PathItem()

  private def restRpcPathItem(method: Method): PathItem =
    PathItem()

  private def toComponents(): Option[Components] = None
}

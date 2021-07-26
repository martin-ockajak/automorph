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
   * @param serverUrls server URLs
   * @return OpenAPI specification
   */
  def jsonRpc(methods: Map[String, Method], info: Info, serverUrls: Seq[String] = Seq()): Specification = {
    val servers = createServers(serverUrls)
    val paths = createPaths(methods, true)
    val components = createComponents()
    Specification(info = info, servers = servers, paths = paths, components = components)
  }

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param title API title
   * @param version API specification version
   * @return OpenAPI specification
   */
  def jsonRpc(
    methods: Map[String, Method],
    title: String,
    version: String
  ): Specification = jsonRpc(methods, Info(title = title, version = version))

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param info OpenAPI Info object
   * @param serverUrls server URLs
   * @return OpenAPI specification
   */
  def restRpc(methods: Map[String, Method], info: Info, serverUrls: Seq[String] = Seq()): Specification = {
    val servers = createServers(serverUrls)
    val paths = createPaths(methods, false)
    val components = createComponents()
    Specification(info = info, servers = servers, paths = paths, components = components)
  }

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param title API title
   * @param version API specification version
   * @return OpenAPI specification
   */
  def restRpc(
    methods: Map[String, Method],
    title: String,
    version: String
  ): Specification = restRpc(methods, Info(title = title, version = version))

  private def createServers(serverUrls: Seq[String]): Option[Servers] = serverUrls match {
    case Seq() => None
    case Seq(urls: _*) => Some(urls.map(url => Server(url = url)).toList.toList)
  }

  private def createPaths(methods: Map[String, Method], rpc: Boolean): Option[Paths] = methods match {
    case noMethods if noMethods.isEmpty => None
    case actualMethods =>
      Some(actualMethods.map { case (name, method) =>
        val path = s"/${name.replace('.', '/')}"
        val pathItem = if (rpc) jsonRpcPathItem(method) else restRpcPathItem(method)
        path -> pathItem
      }.toMap)
  }

  private def jsonRpcPathItem(method: Method): PathItem = {
    PathItem()
  }

  private def restRpcPathItem(method: Method): PathItem = {
    PathItem()
  }

  private def createComponents(): Option[Components] = None
}

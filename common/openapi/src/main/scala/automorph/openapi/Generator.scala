package automorph.openapi

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
   * @return OpenAPI specification
   */
  def jsonRpc(methods: Map[String, Method], info: Info): OpenApi = ???

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param title API title
   * @param version API specification version
   * @return OpenAPI specification
   */
  def jsonRpc(methods: Map[String, Method], title: String, version: String): OpenApi =
    jsonRpc(methods, Info(title = title, version = version))

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param info OpenAPI Info object
   * @return OpenAPI specification
   */
  def restRpc(methods: Map[String, Method], info: Info): OpenApi = ???

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param title API title
   * @param version API specification version
   * @return OpenAPI specification
   */
  def restRpc(methods: Map[String, Method], title: String, version: String): OpenApi =
    restRpc(methods, Info(title = title, version = version))
}

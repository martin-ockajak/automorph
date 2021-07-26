package automorph.openapi

import automorph.util.Method

case object Generator {
  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @return OpenAPI specification
   */
  def jsonRpc(methods: Map[String, Method]): OpenApi = ???

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @return OpenAPI specification
   */
  def restRpc(methods: Map[String, Method]): OpenApi = ???
}

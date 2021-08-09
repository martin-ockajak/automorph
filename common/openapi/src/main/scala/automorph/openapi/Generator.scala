package automorph.openapi

import automorph.openapi.Specification.{Components, Paths, Servers}
import automorph.openapi.Schema.Properties
import automorph.util.{Method, Parameter}
import io.circe.generic.auto._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}
import javax.swing.plaf.synth.SynthCheckBoxMenuItemUI

/**
 * Open API specification generator.
 *
 * @see https://github.com/OAI/OpenAPI-Specification
 */
case object Generator {

  private val contentType = "application/json"
  private val objectType = "object"
  private val httpStatusCodeOk = 200.toString
  private val requestTitle = "Request"
  private val resultTitle = "Result"
  private val errorTitle = "Error"
  private val jsonRpcPrefix = "JSON-RPC "
  private val restRpcPrefix = "REST-RPC "
  private val argumentsDescription = "Invoked method argument values by name"
  private val scaladocMarkup = "^[/\\* ]*$".r
  private val optionTypePrefix = s"${classOf[Option[Unit]].getName}"
//  implicit private val pathsEncoder: Encoder[Paths] = new Encoder[Paths] {
//    def apply(v: Paths): Json = Json.obj((v.map { case (key, value) =>
//      key -> value.asJson
//    }.toSeq)*)
//  }
//  implicit private val pathsEncoder: Encoder[Paths] = Encoder.encodeMap
//  implicit private val pathsDecoder: Decoder[Paths] = Decoder.decodeMap
//  implicit private val componentsEncoder: Encoder[Components] = Encoder.encodeMap
//  implicit private val componentsDecoder: Decoder[Components] = Decoder.decodeMap
//    implicit private val propertiesEncoder: Encoder[Properties] = Encoder.encodeMap
//    implicit private val propertiesDecoder: Decoder[Properties] = Decoder.decodeMap

  /**
   * Generate OpenAPI paths for given API methods.
   *
   * @param methods named API
   * @return OpenAPI paths
   */
  def jsonRpcPaths(methods: Map[String, Method]): Paths = toPaths(methods, true)

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param title API title
   * @param version API specification version
   * @param serverUrls API server URL
   * @return OpenAPI specification
   */
  def jsonRpcSpec(
    methods: Map[String, Method],
    title: String,
    version: String,
    serverUrls: Seq[String]
  ): Specification = Specification(
    paths = Some(toPaths(methods, true)),
    info = Info(title = title, version = version),
    servers = toServers(serverUrls),
    components = toComponents()
  )

  /**
   * Generate OpenAPI paths for given API methods.
   *
   * @param methods named API methods
   * @return OpenAPI paths
   */
  def restRpcPaths(methods: Map[String, Method]): Paths = toPaths(methods, false)

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param title API title
   * @param version API specification version
   * @param serverUrls API server URL
   * @return OpenAPI specification
   */
  def restRpcSpec(
    methods: Map[String, Method],
    title: String,
    version: String,
    serverUrls: Seq[String]
  ): Specification = Specification(
    paths = Some(toPaths(methods, false)),
    info = Info(title = title, version = version),
    servers = toServers(serverUrls),
    components = toComponents()
  )

  /**
   * Serialize OpenAPI specification into JSON codec.
   *
   * @param specification OpenApi specification
   * @return OpenAPI specification in JSON codec
   */
  def json(specification: Specification): String = {
//    Schema().asJson
//    specification.asJson.spaces2
    ???
  }

  private def parameterSchemas(method: Method): Map[String, Schema] =
    method.parameters.flatten.map { parameter =>
      // FIXME - convert data type to JSON type
      parameter.name -> Schema(
        Some(parameter.dataType),
        Some(parameter.name),
        scaladocField(method.documentation, s"param ${parameter.name}")
      )
    }.toMap

  private def resultSchema(method: Method): Schema =
    // FIXME - convert data type to JSON type
    Schema(Some(method.resultType), Some("result"), scaladocField(method.documentation, "return"))

  private def requiredParameters(method: Method): List[String] =
    method.parameters.flatten.filter(_.dataType.startsWith(optionTypePrefix)).map(_.name).toList

  private def jsonRpcRequestSchema(method: Method): Schema = Schema(
    Some(objectType),
    Some(requestTitle),
    Some(s"$jsonRpcPrefix$requestTitle"),
    Some(Map(
      "jsonrpc" -> Schema(Some("string"), Some("jsonrpc"), Some("Protocol version (must be 2.0)")),
      "method" -> Schema(Some("string"), Some("method"), Some("Invoked method name")),
      "params" -> Schema(
        Some(objectType),
        Some(method.name),
        Some(argumentsDescription),
        maybe(parameterSchemas(method)),
        maybe(requiredParameters(method))
      ),
      "id" -> Schema(
        Some("integer"),
        Some("id"),
        Some("Call identifier, a request without and identifier is considered to be a notification")
      )
    )),
    Some(List("jsonrpc", "method", "params"))
  )

  private def restRpcRequestSchema(method: Method): Schema =
    Schema(Some(objectType), Some(method.name), Some(argumentsDescription), maybe(parameterSchemas(method)))

  private def jsonRpcResultSchema(method: Method): Schema = Schema(
    Some(objectType),
    Some(resultTitle),
    Some(s"$jsonRpcPrefix$resultTitle"),
    Some(Map("result" -> resultSchema(method))),
    Some(List("result"))
  )

  private def restRpcResultSchema(method: Method): Schema = Schema(
    Some(objectType),
    Some(resultTitle),
    Some(s"$restRpcPrefix$resultTitle"),
    Some(Map("result" -> resultSchema(method))),
    Some(List("result"))
  )

  private def jsonRpcErrorSchema: Schema = Schema(
    Some(objectType),
    Some(errorTitle),
    Some(s"$jsonRpcPrefix$errorTitle"),
    Some(Map(
      "error" -> Schema(
        Some("string"),
        Some("error"),
        Some("Failed method call error details"),
        Some(Map(
          "code" -> Schema(Some("integer"), Some("code"), Some("Error code")),
          "message" -> Schema(Some("string"), Some("message"), Some("Error message")),
          "data" -> Schema(Some("object"), Some("data"), Some("Additional error incodecion"))
        )),
        Some(List("code", "message"))
      )
    )),
    Some(List("error"))
  )

  private def restRpcErrorSchema: Schema = Schema(
    Some(objectType),
    Some(errorTitle),
    Some(s"$restRpcPrefix$errorTitle"),
    Some(Map(
      "error" -> Schema(
        Some("string"),
        Some("error"),
        Some("Failed method call error details"),
        Some(Map(
          "code" -> Schema(Some("integer"), Some("code"), Some("Error code")),
          "message" -> Schema(Some("string"), Some("message"), Some("Error message")),
          "data" -> Schema(Some("object"), Some("data"), Some("Additional error incodecion"))
        )),
        Some(List("code", "message"))
      )
    )),
    Some(List("error"))
  )

  private def scaladocField(scaladoc: Option[String], field: String): Option[String] = {
    val fieldPrefix = s"$field "
    scaladoc.flatMap { doc =>
      maybe(doc.split('\n').flatMap { line =>
        line.split('@') match {
          case Array(prefix, tag, rest @ _*) if tag.startsWith(fieldPrefix) =>
            Some((tag.substring(fieldPrefix.size) +: rest).mkString("@").trim)
          case _ => None
        }
      })
    }.map(_.mkString(" "))
  }

  private def toServers(serverUrls: Seq[String]): Option[Servers] =
    maybe(serverUrls.map(url => Server(url = url)).toList)

  private def toPaths(methods: Map[String, Method], rpc: Boolean): Paths = methods.map { case (name, method) =>
    val (requestSchema, resultSchema, errorSchema) =
      if (rpc) {
        (jsonRpcRequestSchema(method), jsonRpcResultSchema(method), jsonRpcErrorSchema)
      } else {
        (restRpcRequestSchema(method), restRpcResultSchema(method), restRpcErrorSchema)
      }
    val requestMediaType = MediaType(schema = Some(requestSchema))
    val resultMediaType = MediaType(schema = Some(resultSchema))
    val errorMediaType = MediaType(schema = Some(errorSchema))
    val requestBody = RequestBody(content = Map(contentType -> requestMediaType), required = Some(true))
    val responses = Map(
      "default" -> Response("Failed method call error details", Some(Map(contentType -> errorMediaType))),
      httpStatusCodeOk -> Response("Succesful method call result value", Some(Map(contentType -> resultMediaType)))
    )
    val operation = Operation(requestBody = Some(requestBody), responses = Some(responses))
    val summary = method.documentation.flatMap(_.split('\n').find {
      case scaladocMarkup(_*) => true
      case _ => false
    }.map(_.trim))
    val description = method.documentation
    val path = s"/${name.replace('.', '/')}"
    val pathItem = PathItem(post = Some(operation), summary = summary, description = description)
    path -> pathItem
  }

  private def toComponents(): Option[Components] = None

  private def maybe[T <: Iterable[_]](iterable: T): Option[T] = if (iterable.isEmpty) None else Some(iterable)
}

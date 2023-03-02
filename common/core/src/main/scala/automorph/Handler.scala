package automorph

import automorph.RpcException.{FunctionNotFoundException, InvalidRequestException}
import automorph.handler.meta.HandlerMeta
import automorph.handler.{HandlerBinding, HandlerResult, ProtocolHandlerBuilder, SystemHandlerBuilder}
import automorph.log.{LogProperties, Logging}
import automorph.spi.protocol.{RpcFunction, RpcMessage, RpcRequest}
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
import automorph.util.Extensions.{EffectOps, TryOps}
import java.io.InputStream
import scala.collection.immutable.ListMap
import scala.util.{Failure, Success, Try}

/**
 * RPC request handler.
 *
 * The handler can be used to convert remote API calls or one-way messages into type-safe invocations of API instances.
 *
 * It provides automatic derivation of remote API RPC bindings for existing API implementations and processing of
 * incoming RPC requests into API invocations resulting in corresponding RPC responses.
 *
 * Used by RPC servers to invoke bound API methods based on incoming requests.
 *
 * @constructor
 *   Creates a new RPC request handler with specified system and protocol plugins providing corresponding message
 *   context type.
 * @param rpcProtocol
 *   RPC protocol plugin
 * @param effectSystem
 *   effect system plugin
 * @param mapName
 *   maps API schema function to the exposed remote function name (empty result causes the method not to be exposed)
 * @param apiBindings
 *   API method bindings
 * @tparam Node
 *   message node type
 * @tparam Codec
 *   message codec plugin type
 * @tparam Effect
 *   effect type
 * @tparam Context
 *   message context type
 */
final case class Handler[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  rpcProtocol: RpcProtocol[Node, Codec, Context],
  effectSystem: EffectSystem[Effect],
  mapName: String => Iterable[String] = Seq(_),
  apiBindings: ListMap[String, HandlerBinding[Node, Effect, Context]] =
    ListMap[String, HandlerBinding[Node, Effect, Context]](),
) extends HandlerMeta[Node, Codec, Effect, Context] with Logging {

  /** Bound remote functions. */
  lazy val functions: Seq[RpcFunction] = bindings.map { case (name, binding) => binding.function.copy(name = name) }
    .toSeq
  implicit private val givenSystem: EffectSystem[Effect] = effectSystem
  private val bindings = (schemaBindings ++ apiBindings).flatMap { case (name, binding) =>
    mapName(name).map(_ -> binding)
  }

  private[automorph] def clone(
    apiBindings: ListMap[String, HandlerBinding[Node, Effect, Context]]
  ): Handler[Node, Codec, Effect, Context] =
    copy(apiBindings = apiBindings)

  /**
   * Processes an RPC request by invoking a bound remote function based on the specified RPC request and its context and
   * return an RPC response.
   *
   * @param requestBody
   *   request message body
   * @param requestContext
   *   request context
   * @param requestId
   *   request correlation identifier
   * @return
   *   optional response message
   */
  def processRequest(
    requestBody: InputStream,
    requestContext: Context,
    requestId: String,
  ): Effect[HandlerResult[Context]] =
    // Parse request
    rpcProtocol.parseRequest(requestBody, requestContext, requestId).fold(
      error =>
        errorResponse(
          error.exception,
          error.message,
          responseRequired = true,
          ListMap(LogProperties.requestId -> requestId),
        ),
      rpcRequest => {
        // Invoke requested remote function
        lazy val requestProperties = ListMap(LogProperties.requestId -> requestId) ++ rpcRequest.message.properties
        lazy val allProperties = requestProperties ++ rpcRequest.message.text.map(LogProperties.messageBody -> _)
        logger.trace(s"Received ${rpcProtocol.name} request", allProperties)
        callFunction(rpcRequest, requestContext, requestProperties)
      },
    )

  /**
   * Creates a copy of this handler with specified global bound API method name mapping function.
   *
   * Bound API methods are exposed using their transformed via the `mapName` function. The `mapName` function is applied
   * globally to the results to all bound APIs and their specific name mapping provided by the 'bind' method.
   *
   * @param mapName
   *   maps API method name to the exposed remote function name (empty result causes the method not to be exposed)
   * @return
   *   RPC request handler with specified global API method name mapping
   */
  def mapName(mapName: String => Iterable[String]): Handler[Node, Codec, Effect, Context] =
    copy(mapName = mapName)

  override def toString: String = {
    val plugins = Map[String, Any]("system" -> effectSystem, "protocol" -> rpcProtocol).map { case (name, plugin) =>
      s"$name = ${plugin.getClass.getName}"
    }.mkString(", ")
    s"${this.getClass.getName}($plugins)"
  }

  /**
   * Calls bound remote function specified in a request and creates a response.
   *
   * Optional request context is used as a last remote function argument.
   *
   * @param rpcRequest
   *   RPC request
   * @param context
   *   request context
   * @param requestProperties
   *   request properties
   * @return
   *   bound function call RPC response
   */
  private def callFunction(
    rpcRequest: RpcRequest[Node, rpcProtocol.Metadata, Context],
    context: Context,
    requestProperties: => Map[String, String],
  ): Effect[HandlerResult[Context]] = {
    // Lookup bindings for the specified remote function
    val responseRequired = rpcRequest.responseRequired
    logger.debug(s"Processing ${rpcProtocol.name} request", requestProperties)
    bindings.get(rpcRequest.function).map { binding =>
      // Extract bound function argument nodes
      extractArguments(rpcRequest, binding).map { argumentNodes =>
        // Decode bound function arguments
        decodeArguments(argumentNodes, binding)
      }.map { arguments =>
        // Call bound function
        binding.call(arguments, context)
      }.pureFold(
        error => errorResponse(error, rpcRequest.message, responseRequired, requestProperties),
        result => {
          // Encode bound function result
          val contextualResultNode = result.asInstanceOf[Effect[Any]].map { resultValue =>
            encodeResult(resultValue, binding)
          }

          // Create RPC response
          resultResponse(contextualResultNode, rpcRequest, requestProperties)
        },
      )
    }.getOrElse {
      val error = FunctionNotFoundException(s"Function not found: ${rpcRequest.function}", None.orNull)
      errorResponse(error, rpcRequest.message, responseRequired, requestProperties)
    }
  }

  /**
   * Validates and extracts specified bound remote function arguments from a request.
   *
   * Optional request context is used as a last remote function argument.
   *
   * @param rpcRequest
   *   RPC request
   * @param binding
   *   remote function binding
   * @return
   *   bound function arguments
   */
  private def extractArguments(
    rpcRequest: RpcRequest[Node, ?, Context],
    binding: HandlerBinding[Node, Effect, Context],
  ): Try[Seq[Option[Node]]] = {
    // Adjust expected function parameters if it uses context as its last parameter
    val parameters = binding.function.parameters
    val parameterNames = parameters.map(_.name).dropRight(if (binding.acceptsContext) 1 else 0)

    // Identify redundant arguments
    val namedArguments = rpcRequest.arguments.flatMap(_.toOption).toMap
    val positionalArguments = rpcRequest.arguments.flatMap(_.swap.toOption)
    val argumentNames = namedArguments.keys.toSeq
    val matchedNamedArguments = argumentNames.intersect(parameterNames)
    val requiredPositionalArguments = parameterNames.size - matchedNamedArguments.size
    val redundantNames = argumentNames.diff(parameterNames)
    val redundantIndices = Range(requiredPositionalArguments, positionalArguments.size)

    // Assemble required arguments
    if (redundantNames.size + redundantIndices.size > 0) {
      val redundantIdentifiers = redundantNames ++ redundantIndices.map(_.toString)
      Failure(new IllegalArgumentException(s"Redundant arguments: ${redundantIdentifiers.mkString(", ")}"))
    } else {
      Success(parameterNames.foldLeft(Seq[Option[Node]]() -> 0) { case ((arguments, index), name) =>
        val (argument, newIndex) = namedArguments.get(name) match {
          case Some(value) => Some(value) -> index
          case _ if index < positionalArguments.size => Some(positionalArguments(index)) -> (index + 1)
          case _ => None -> index
        }
        (arguments :+ argument) -> newIndex
      }._1)
    }
  }

  /**
   * Decodes specified bound remote function argument nodes into values.
   *
   * @param argumentNodes
   *   bound remote function argument nodes
   * @param binding
   *   remote function binding
   * @return
   *   bound remote function arguments
   */
  private def decodeArguments(
    argumentNodes: Seq[Option[Node]],
    binding: HandlerBinding[Node, Effect, Context],
  ): Seq[Any] =
    binding.function.parameters.zip(argumentNodes).map { case (parameter, argumentNode) =>
      val decodeArgument = binding.argumentDecoders.getOrElse(
        parameter.name,
        throw new IllegalStateException(s"Missing method parameter decoder: ${parameter.name}"),
      )
      Try(Option(decodeArgument(argumentNode)).get).recoverWith { case error =>
        Failure(InvalidRequestException(
          s"${argumentNode.fold("Missing")(_ => "Malformed")} argument: ${parameter.name}",
          error,
        ))
      }.get
    }

  /**
   * Decodes specified bound remote function argument nodes into values.
   *
   * @param result
   *   bound remote function result
   * @param binding
   *   remote function binding
   * @return
   *   bound remote function result node
   */
  private def encodeResult(result: Any, binding: HandlerBinding[Node, Effect, Context]): (Node, Option[Context]) =
    Try(binding.encodeResult(result)).recoverWith { case error =>
      Failure(new IllegalArgumentException("Malformed result", error))
    }.get

  /**
   * Creates a response for bound remote function call result.
   *
   * @param callResult
   *   remote function call result
   * @param rpcRequest
   *   RPC request
   * @param requestProperties
   *   request properties
   * @return
   *   bound function call RPC response
   */
  private def resultResponse(
    callResult: Effect[(Node, Option[Context])],
    rpcRequest: RpcRequest[Node, rpcProtocol.Metadata, Context],
    requestProperties: => Map[String, String],
  ): Effect[HandlerResult[Context]] =
    callResult.either.flatMap { result =>
      result.fold(
        error => logger.error(s"Failed to process ${rpcProtocol.name} request", error, requestProperties),
        _ => logger.info(s"Processed ${rpcProtocol.name} request", requestProperties),
      )

      // Create response
      Option.when(rpcRequest.responseRequired)(response(result.toTry, rpcRequest.message, requestProperties))
        .getOrElse {
          val responseContext = result.toOption.flatMap(_._2)
          effectSystem.successful(HandlerResult(None, None, responseContext))
        }
    }

  /**
   * Creates a handler result containing an RPC response for the specified error.
   *
   * @param error
   *   exception
   * @param message
   *   RPC message
   * @param responseRequired
   *   true if response is required
   * @param requestProperties
   *   request properties
   * @return
   *   handler result
   */
  private def errorResponse(
    error: Throwable,
    message: RpcMessage[rpcProtocol.Metadata],
    responseRequired: Boolean,
    requestProperties: => Map[String, String],
  ): Effect[HandlerResult[Context]] = {
    logger.error(s"Failed to process ${rpcProtocol.name} request", error, requestProperties)
    Option.when(responseRequired)(response(Failure(error), message, requestProperties)).getOrElse {
      effectSystem.successful(HandlerResult(None, None, None))
    }
  }

  /**
   * Creates a handler result containing an RPC response for the specified resul value.
   *
   * @param result
   *   a call result on success or an exception on failure
   * @param message
   *   RPC message
   * @param requestProperties
   *   request properties
   * @return
   *   handler result
   */
  private def response(
    result: Try[(Node, Option[Context])],
    message: RpcMessage[rpcProtocol.Metadata],
    requestProperties: => Map[String, String],
  ): Effect[HandlerResult[Context]] = {
    rpcProtocol.createResponse(result.map(_._1), message.metadata).pureFold(
      error => effectSystem.failed(error),
      rpcResponse => {
        val responseBody = rpcResponse.message.body
        lazy val allProperties = rpcResponse.message.properties ++ requestProperties ++
          rpcResponse.message.text.map(LogProperties.messageBody -> _)
        logger.trace(s"Sending ${rpcProtocol.name} response", allProperties)
        effectSystem.successful(HandlerResult(Some(responseBody), result.failed.toOption, result.toOption.flatMap(_._2)))
      },
    )
  }

  private def schemaBindings: ListMap[String, HandlerBinding[Node, Effect, Context]] =
    ListMap(rpcProtocol.apiSchemas.map { apiSchema =>
      val describedFunctions = rpcProtocol.apiSchemas.filter { apiSchema =>
        !apiBindings.contains(apiSchema.function.name)
      }.map(_.function) ++ apiBindings.values.map(_.function)
      apiSchema.function.name -> HandlerBinding[Node, Effect, Context](
        apiSchema.function,
        Map.empty,
        result => result.asInstanceOf[Node] -> None,
        (_, _) => effectSystem.successful(apiSchema.invoke(describedFunctions)),
        acceptsContext = false,
      )
    }*)
}

object Handler {

  /** Handler with arbitrary node type. */
  type AnyCodec[Effect[_], Context] = Handler[?, ?, Effect, Context]

  /**
   * Creates an RPC request handler builder with specified RPC protocol plugin.
   *
   * @param protocol
   *   RPC protocol plugin
   * @tparam Node
   *   message node type
   * @tparam Codec
   *   message codec plugin type
   * @tparam Context
   *   message context type
   * @return
   *   RPC request handler builder
   */
  def protocol[Node, Codec <: MessageCodec[Node], Context](
    protocol: RpcProtocol[Node, Codec, Context]
  ): ProtocolHandlerBuilder[Node, Codec, Context] =
    ProtocolHandlerBuilder(protocol)

  /**
   * Creates an RPC request handler builder with specified effect system plugin.
   *
   * @param system
   *   effect system plugin
   * @tparam Effect
   *   effect type
   * @return
   *   RPC request handler builder
   */
  def system[Effect[_]](system: EffectSystem[Effect]): SystemHandlerBuilder[Effect] =
    SystemHandlerBuilder(system)
}

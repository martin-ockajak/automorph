package automorph

import automorph.handler.meta.HandlerMeta
import automorph.handler.{HandlerBinding, HandlerResult, ProtocolHandlerBuilder, SystemHandlerBuilder}
import automorph.log.{LogProperties, Logging}
import automorph.spi.RpcProtocol.FunctionNotFoundException
import automorph.spi.protocol.{RpcDiscover, RpcFunction, RpcMessage, RpcRequest}
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
import automorph.util.Extensions.{EffectOps, TryOps}
import automorph.util.{Bytes, CannotEqual}
import scala.collection.immutable.{ArraySeq, ListMap}
import scala.util.{Failure, Success, Try}

/**
 * RPC request handler.
 *
 * Used by RPC servers to invoke bound API methods based on incoming requests.
 *
 * @constructor Creates a new RPC request handler with specified system and protocol plugins providing corresponding message context type.
 * @param protocol RPC protocol plugin
 * @param system effect system plugin
 * @param bindings API method bindings
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context message context type
 */
final case class Handler[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  protocol: RpcProtocol[Node, Codec, Context],
  system: EffectSystem[Effect],
  bindings: ListMap[String, HandlerBinding[Node, Effect, Context]] =
    ListMap.empty[String, HandlerBinding[Node, Effect, Context]]
) extends HandlerMeta[Node, Codec, Effect, Context] with CannotEqual with Logging {

  private val discoveryBindings = createDiscoveryBindings
  private val allBindings = bindings ++ discoveryBindings.view.mapValues(_._1).toSeq
  implicit private val givenSystem: EffectSystem[Effect] = system

  /** Bound RPC functions. */
  lazy val functions: Seq[RpcFunction] = allBindings.map { case (name, binding) =>
    binding.function.copy(name = name)
  }.toSeq

  /**
   * Processes an RPC request by invoking a bound RPC function based on the specified RPC request and its context and return an RPC response.
   *
   * @param requestBody request message body
   * @param requestContext request context
   * @param requestId request correlation identifier
   * @tparam MessageBody message body type
   * @return optional response message
   */
  def processRequest[MessageBody: Bytes](
    requestBody: MessageBody,
    requestContext: Context,
    requestId: String
  ): Effect[HandlerResult[MessageBody, Context]] = {
    // Parse request
    val requestMessageBody = implicitly[Bytes[MessageBody]].from(requestBody)
    protocol.parseRequest(requestMessageBody, requestContext, requestId).fold(
      error => errorResponse(error.exception, error.message, true, ListMap(LogProperties.requestId -> requestId)),
      rpcRequest => {
        // Invoke requested RPC function
        lazy val requestProperties = ListMap(
          LogProperties.requestId -> requestId
        ) ++ rpcRequest.message.properties + (
          LogProperties.messageSize -> requestMessageBody.length.toString
        )
        lazy val allProperties = requestProperties ++ rpcRequest.message.text.map(LogProperties.messageBody -> _)
        logger.trace(s"Received ${protocol.name} request", allProperties)
        callFunction(rpcRequest, requestContext, requestProperties)
      }
    )
  }

  override def toString: String = {
    val plugins = Map(
      "system" -> system,
      "protocol" -> protocol
    ).map { case (name, plugin) => s"$name = ${plugin.getClass.getName}" }.mkString(", ")
    s"${this.getClass.getName}($plugins)"
  }

  /**
   * Calls bound RPC function specified in a request and creates a response.
   *
   * Optional request context is used as a last RPC function argument.
   *
   * @param rpcRequest RPC request
   * @param context request context
   * @param requestId request correlation idendifier
   * @param requestProperties request properties
   * @tparam MessageBody message body type
   * @return bound function call RPC response
   */
  private def callFunction[MessageBody: Bytes](
    rpcRequest: RpcRequest[Node, protocol.Metadata],
    context: Context,
    requestProperties: => Map[String, String]
  ): Effect[HandlerResult[MessageBody, Context]] = {
    // Lookup bindings for the specified RPC function
    val responseRequred = rpcRequest.responseRequired
    logger.debug(s"Processing ${protocol.name} request", requestProperties)
    allBindings.get(rpcRequest.function).map { handlerBinding =>
      // Extract arguments
      extractArguments(rpcRequest, handlerBinding).pureFold(
        error => errorResponse(error, rpcRequest.message, responseRequred, requestProperties),
        arguments =>
          discoveryBindings.get(rpcRequest.function).map { case (_, apiSpecification) =>
            // Retrieve the API specification
            directResponse(apiSpecification(rpcRequest.message.metadata), rpcRequest, requestProperties)
          }.getOrElse {
            // Invoke bound function
            Try(handlerBinding.invoke(arguments, context).either).pureFold(
              error => errorResponse(error, rpcRequest.message, responseRequred, requestProperties),
              result => resultResponse(result, rpcRequest, requestProperties)
            )
          }
      )
    }.getOrElse {
      val error = FunctionNotFoundException(s"Function not found: ${rpcRequest.function}", None.orNull)
      errorResponse(error, rpcRequest.message, responseRequred, requestProperties)
    }
  }

  /**
   * Validates and extracts specified bound RPC function arguments from a request.
   *
   * Optional request context is used as a last RPC function argument.
   *
   * @param rpcRequest RPC request
   * @param handlerBinding handler RPC function binding
   * @return bound function arguments
   */
  private def extractArguments(
    rpcRequest: RpcRequest[Node, _],
    handlerBinding: HandlerBinding[Node, Effect, Context]
  ): Try[Seq[Option[Node]]] = {
    // Adjust expected function parameters if it uses context as its last parameter
    val parameters = handlerBinding.function.parameters
    val parameterNames = parameters.map(_.name).dropRight(if (handlerBinding.acceptsContext) 1 else 0)
    rpcRequest.arguments.fold(
      positionalArguments => {
        // Arguments by position
        val redundantSize = positionalArguments.size - parameterNames.size
        if (redundantSize > 0) {
          Failure(new IllegalArgumentException(
            s"Redundant arguments: ${Range(parameterNames.size, redundantSize).mkString(", ")}"
          ))
        } else {
          Success(positionalArguments.map(Some(_)) ++ Seq.fill(-redundantSize)(None))
        }
      },
      namedArguments => {
        // Arguments by name
        val redundantNames = namedArguments.keys.toSeq.diff(parameterNames)
        if (redundantNames.nonEmpty) {
          Failure(new IllegalArgumentException(s"Redundant arguments: ${redundantNames.mkString(", ")}"))
        } else {
          Success(parameterNames.map(namedArguments.get))
        }
      }
    )
  }

  /**
   * Creates a response for bound RPC function call result.
   *
   * @param callResult RPC function call result
   * @param rpcRequest RPC request
   * @param requestProperties request properties
   * @tparam MessageBody message body type
   * @return bound function call RPC response
   */
  private def resultResponse[MessageBody: Bytes](
    callResult: Effect[Either[Throwable, (Node, Option[Context])]],
    rpcRequest: RpcRequest[Node, protocol.Metadata],
    requestProperties: => Map[String, String]
  ): Effect[HandlerResult[MessageBody, Context]] =
    callResult.flatMap { result =>
      result.fold(
        error => logger.error(s"Failed to process ${protocol.name} request", error, requestProperties),
        _ => logger.info(s"Processed ${protocol.name} request", requestProperties)
      )

      // Create response
      Option.when(rpcRequest.responseRequired) {
        response(result.toTry, rpcRequest.message, requestProperties)
      }.getOrElse {
        val responseContext = result.toOption.flatMap(_._2)
        system.pure(HandlerResult(None, None, responseContext))
      }
    }

  /**
   * Creates a handler result containing an RPC response for the specified error.
   *
   * @param error exception
   * @param message RPC message
   * @param responseRequired true if response is required
   * @param requestProperties request properties
   * @tparam MessageBody message body type
   * @return handler result
   */
  private def errorResponse[MessageBody: Bytes](
    error: Throwable,
    message: RpcMessage[protocol.Metadata],
    responseRequired: Boolean,
    requestProperties: => Map[String, String]
  ): Effect[HandlerResult[MessageBody, Context]] = {
    logger.error(s"Failed to process ${protocol.name} request", error, requestProperties)
    Option.when(responseRequired) {
      response(Failure(error), message, requestProperties)
    }.getOrElse {
      system.pure(HandlerResult(None, None, None))
    }
  }

  /**
   * Creates a specified response direcly.
   *
   * @param responseBody RPC response body
   * @param rpcRequest RPC request
   * @param requestProperties request properties
   * @tparam MessageBody message body type
   * @return direct RPC response
   */
  private def directResponse[MessageBody: Bytes](
    responseBody: ArraySeq.ofByte,
    rpcRequest: RpcRequest[Node, protocol.Metadata],
    requestProperties: => Map[String, String]
  ): Effect[HandlerResult[MessageBody, Context]] = {
    logger.info(s"Processed ${protocol.name} request", requestProperties)
    // Create response
    Option.when(rpcRequest.responseRequired) {
      lazy val allProperties = requestProperties + (LogProperties.messageBody -> Bytes.string.to(responseBody))
      logger.trace(s"Sending ${protocol.name} response", allProperties)
      val responseMessageBody = Some(implicitly[Bytes[MessageBody]].to(responseBody))
      system.pure(HandlerResult(responseMessageBody, None, None))
    }.getOrElse {
      system.pure(HandlerResult(None, None, None))
    }
  }

  /**
   * Creates a handler result containing an RPC response for the specified resul value.
   *
   * @param result a call result on success or an exception on failure
   * @param message RPC message
   * @param requestProperties request properties
   * @tparam MessageBody message body type
   * @return handler result
   */
  private def response[MessageBody: Bytes](
    result: Try[(Node, Option[Context])],
    message: RpcMessage[protocol.Metadata],
    requestProperties: => Map[String, String]
  ): Effect[HandlerResult[MessageBody, Context]] =
    protocol.createResponse(result.map(_._1), message.metadata).pureFold(
      error => system.failed(error),
      rpcResponse => {
        val responseBody = rpcResponse.message.body
        lazy val allProperties = rpcResponse.message.properties ++ requestProperties ++
          rpcResponse.message.text.map(LogProperties.messageBody -> _)
        logger.trace(s"Sending ${protocol.name} response", allProperties)
        val responseMessageBody = Some(implicitly[Bytes[MessageBody]].to(responseBody))
        system.pure(HandlerResult(responseMessageBody, result.failed.toOption, result.toOption.flatMap(_._2)))
      }
    )

  private def createDiscoveryBindings
    : ListMap[String, (HandlerBinding[Node, Effect, Context], protocol.Metadata => ArraySeq.ofByte)] = {
    Option.when(true) {
      ListMap(protocol.discovery.map { discover =>
        val binding = HandlerBinding[Node, Effect, Context](
          discover.function,
          (_, _) => system.pure((None.orNull, None).asInstanceOf[(Node, Option[Context])]),
          false
        )
        val apiSpecification = (metadata: protocol.Metadata) => {
          discover.apiSpecification(bindings.values.toSeq.map(_.function), metadata)
        }
        binding.function.name -> (binding -> apiSpecification)
      }*)
    }.getOrElse(ListMap.empty)
  }
}

object Handler {

  /** Handler with arbitrary node type. */
  type AnyCodec[Effect[_], Context] = Handler[_, _, Effect, Context]

  /**
   * Creates an RPC request handler builder with specified RPC protocol plugin.
   *
   * @param protocol RPC protocol plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Context message context type
   * @return RPC request handler builder
   */
  def protocol[Node, Codec <: MessageCodec[Node], Context](
    protocol: RpcProtocol[Node, Codec, Context]
  ): ProtocolHandlerBuilder[Node, Codec, Context] =
    ProtocolHandlerBuilder(protocol)

  /**
   * Creates an RPC request handler builder with specified effect system plugin.
   *
   * @param system effect system plugin
   * @tparam Effect effect type
   * @return RPC request handler builder
   */
  def system[Effect[_]](system: EffectSystem[Effect]): SystemHandlerBuilder[Effect] =
    SystemHandlerBuilder(system)
}
